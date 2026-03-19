package com.event.application.service;

import com.event.application.dto.attendance.command.AttendEventCommand;
import com.event.application.dto.attendance.external.PointGrantCommand;
import com.event.application.dto.attendance.internal.AttendanceRewardInfo;
import com.event.application.dto.attendance.result.AttendEventResult;
import com.event.application.port.output.EventApplicantRepositoryPort;
import com.event.application.port.output.EventQueryPort;
import com.event.application.port.output.EventRoundPrizeQueryPort;
import com.event.application.port.output.EventRoundQueryPort;
import com.event.application.port.output.PrizeQueryPort;
import com.event.domain.entity.EventApplicantEntity;
import com.event.domain.entity.EventEntity;
import com.event.domain.entity.EventRoundEntity;
import com.event.domain.entity.EventRoundPrizeEntity;
import com.event.domain.entity.PrizeEntity;
import com.event.domain.exception.BusinessException;
import com.event.domain.exception.code.EntryCode;
import com.event.domain.exception.code.EventCode;
import com.event.domain.service.AttendanceDomainService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 출석 응모의 로컬 트랜잭션 흐름을 담당하는 서비스.
 *
 * 처리 순서:
 * 1. 이벤트와 회차를 조회하고 참여 가능 여부를 검증한다.
 * 2. applicant를 저장해 중복 출석을 제어한다.
 * 3. 회차 보상 매핑을 조회하고 지급 가능성을 검증한다.
 * 4. 누적 출석 수를 계산한 뒤 AttendanceProcessor에 로컬 저장과 결과 조립을 위임한다.
 * 5. 보상 매핑이 있으면 point 지급 명령을 after-commit event로 발행한다.
 */
@Service
public class AttendEventTransactionService {

    private static final String IDEMPOTENCY_KEY_FORMAT = "ATTENDANCE:%d:%d:%d";
    private static final long ATTENDANCE_REQUEST_INCREMENT = 1L;

    private final EventQueryPort eventQueryPort;
    private final EventRoundQueryPort eventRoundQueryPort;
    private final EventApplicantRepositoryPort eventApplicantRepositoryPort;
    private final EventRoundPrizeQueryPort eventRoundPrizeQueryPort;
    private final PrizeQueryPort prizeQueryPort;
    private final AttendanceDomainService attendanceDomainService;
    private final AttendanceProcessor attendanceProcessor;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AttendEventTransactionService(
            EventQueryPort eventQueryPort,
            EventRoundQueryPort eventRoundQueryPort,
            EventApplicantRepositoryPort eventApplicantRepositoryPort,
            EventRoundPrizeQueryPort eventRoundPrizeQueryPort,
            PrizeQueryPort prizeQueryPort,
            AttendanceDomainService attendanceDomainService,
            AttendanceProcessor attendanceProcessor,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.eventQueryPort = eventQueryPort;
        this.eventRoundQueryPort = eventRoundQueryPort;
        this.eventApplicantRepositoryPort = eventApplicantRepositoryPort;
        this.eventRoundPrizeQueryPort = eventRoundPrizeQueryPort;
        this.prizeQueryPort = prizeQueryPort;
        this.attendanceDomainService = attendanceDomainService;
        this.attendanceProcessor = attendanceProcessor;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public AttendEventResult attend(AttendEventCommand command) {
        // 1. 회차를 조회하면서 연결된 event를 함께 가져오고 유효한 출석 요청인지 검증한다.
        EventRoundEntity round = eventRoundQueryPort.findById(command.roundId())
                .orElseThrow(() -> BusinessException.from(EventCode.EVENT_ROUND_NOT_FOUND));
        EventEntity event = resolveEvent(round, command.eventId());

        attendanceDomainService.validateAttendable(event, round, Instant.now());

        // 2. applicant 저장으로 같은 회차 재출석을 막는다.
        ApplicantSaveResult applicantSaveResult = saveApplicant(event, round, command.memberId());
        EventApplicantEntity applicant = applicantSaveResult.applicant();

        // 3. 회차에 연결된 보상 매핑을 조회하고 실제 지급 가능한지 확인한다.
        List<EventRoundPrizeEntity> eventRoundPrizes = eventRoundPrizeQueryPort.findActiveByRoundId(round.getId());
        PrizeEntity prize = resolvePrize(eventRoundPrizes);

        attendanceDomainService.validateAttendanceReward(eventRoundPrizes, prize);

        AttendanceRewardInfo rewardInfo = buildRewardInfo(eventRoundPrizes, prize);

        // 4. 응답에 필요한 누적 출석 수를 계산한다.
        // applicant 집합 잠금 뒤 계산한 attendedDays를 그대로 응답 값으로 사용한다.
        long attendedDays = applicantSaveResult.attendedDays();
        long totalDays = eventRoundQueryPort.countByEventId(event.getId());

        // 5. entry/win 저장과 유스케이스 결과 조립은 processor에 위임한다.
        AttendEventResult attendEventResult = attendanceProcessor.process(
                applicant,
                round,
                command.memberId(),
                rewardInfo,
                attendedDays,
                totalDays
        );

        // 6. commit 성공 시에만 listener가 실행되도록 point 지급 명령을 event로 발행한다.
        publishPointRewardCommand(buildPointGrantCommand(event, round, command.memberId(), rewardInfo));
        return attendEventResult;
    }

    private EventEntity resolveEvent(EventRoundEntity round, Long requestedEventId) {
        EventEntity event = round.getEvent();
        if (event.getId().equals(requestedEventId)) {
            return event;
        }

        eventQueryPort.findById(requestedEventId)
                .orElseThrow(() -> BusinessException.from(EventCode.EVENT_NOT_FOUND));
        throw BusinessException.from(EventCode.ROUND_EVENT_MISMATCH);
    }

    private ApplicantSaveResult saveApplicant(
            EventEntity event,
            EventRoundEntity round,
            Long memberId
    ) {
        // 같은 event/member applicant 집합을 같은 조건과 순서로 잠근 뒤 누적 수를 계산한다.
        List<EventApplicantEntity> applicants = eventApplicantRepositoryPort.findByEventIdAndMemberIdForUpdate(
                event.getId(),
                memberId
        );
        long attendedDays = applicants.size() + ATTENDANCE_REQUEST_INCREMENT;

        try {
            EventApplicantEntity applicant = eventApplicantRepositoryPort.save(EventApplicantEntity.create(
                    event,
                    round,
                    memberId,
                    memberId
            ));
            return new ApplicantSaveResult(applicant, attendedDays);
        } catch (DataIntegrityViolationException ex) {
            // applicant unique 충돌은 같은 회차 중복 출석으로 본다.
            throw BusinessException.from(EntryCode.ENTRY_ALREADY_APPLIED);
        }
    }

    private AttendanceRewardInfo buildRewardInfo(
            List<EventRoundPrizeEntity> eventRoundPrizes,
            PrizeEntity prize
    ) {
        // 보상 매핑이 없거나 prize 조회가 실패한 경우엔 무보상 출석으로 처리한다.
        if (eventRoundPrizes.isEmpty() || Objects.isNull(prize)) {
            return null;
        }

        // 현재 출석 회차는 보상 1건만 허용하므로 첫 번째 매핑을 그대로 사용한다.
        EventRoundPrizeEntity eventRoundPrize = eventRoundPrizes.getFirst();
        return AttendanceRewardInfo.of(
                eventRoundPrize.getId(),
                prize.getId(),
                prize.getPrizeName(),
                prize.getRewardType(),
                prize.getPointAmount()
        );
    }

    private PointGrantCommand buildPointGrantCommand(
            EventEntity event,
            EventRoundEntity round,
            Long memberId,
            AttendanceRewardInfo rewardInfo
    ) {
        // point 보상이 없으면 외부 포인트 시스템에 전달할 명령도 만들지 않는다.
        if (Objects.isNull(rewardInfo)) {
            return null;
        }

        // idempotencyKey는 외부 API 중복 지급을 막기 위한 키다.
        return PointGrantCommand.of(
                event.getId(),
                round.getId(),
                memberId,
                rewardInfo.eventRoundPrizeId(),
                rewardInfo.pointAmount(),
                IDEMPOTENCY_KEY_FORMAT.formatted(event.getId(), round.getId(), memberId)
        );
    }

    private void publishPointRewardCommand(PointGrantCommand pointGrantCommand) {
        // 무보상 출석이면 after-commit listener로 넘길 외부 연동 명령도 없다.
        if (Objects.isNull(pointGrantCommand)) {
            return;
        }

        applicationEventPublisher.publishEvent(pointGrantCommand);
    }

    private PrizeEntity resolvePrize(List<EventRoundPrizeEntity> eventRoundPrizes) {
        // 보상 매핑이 없으면 prize 조회도 생략한다.
        if (eventRoundPrizes.isEmpty()) {
            return null;
        }

        // 출석 회차는 단일 보상만 허용하므로 첫 번째 매핑의 prize를 조회한다.
        return prizeQueryPort.findById(eventRoundPrizes.getFirst().getPrizeId())
                .orElse(null);
    }

    private record ApplicantSaveResult(
            EventApplicantEntity applicant,
            long attendedDays
    ) {
    }
}
