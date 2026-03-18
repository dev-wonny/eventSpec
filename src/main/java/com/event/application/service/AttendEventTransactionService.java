package com.event.application.service;

import com.event.application.dto.attendance.command.AttendEventCommand;
import com.event.application.dto.attendance.external.PointGrantCommand;
import com.event.application.dto.attendance.internal.AttendEventTransactionResult;
import com.event.application.dto.attendance.internal.AttendanceRewardInfo;
import com.event.application.dto.attendance.result.AttendEventResult;
import com.event.application.port.output.EventApplicantCommandPort;
import com.event.application.port.output.EventEntryQueryPort;
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
 * 5. 트랜잭션 밖에서 사용할 point 지급 명령을 함께 반환한다.
 */
@Service
public class AttendEventTransactionService {

    private static final String IDEMPOTENCY_KEY_FORMAT = "ATTENDANCE:%d:%d:%d";

    private final EventQueryPort eventQueryPort;
    private final EventRoundQueryPort eventRoundQueryPort;
    private final EventApplicantCommandPort eventApplicantCommandPort;
    private final EventEntryQueryPort eventEntryQueryPort;
    private final EventRoundPrizeQueryPort eventRoundPrizeQueryPort;
    private final PrizeQueryPort prizeQueryPort;
    private final AttendanceDomainService attendanceDomainService;
    private final AttendanceProcessor attendanceProcessor;

    public AttendEventTransactionService(
            EventQueryPort eventQueryPort,
            EventRoundQueryPort eventRoundQueryPort,
            EventApplicantCommandPort eventApplicantCommandPort,
            EventEntryQueryPort eventEntryQueryPort,
            EventRoundPrizeQueryPort eventRoundPrizeQueryPort,
            PrizeQueryPort prizeQueryPort,
            AttendanceDomainService attendanceDomainService,
            AttendanceProcessor attendanceProcessor
    ) {
        this.eventQueryPort = eventQueryPort;
        this.eventRoundQueryPort = eventRoundQueryPort;
        this.eventApplicantCommandPort = eventApplicantCommandPort;
        this.eventEntryQueryPort = eventEntryQueryPort;
        this.eventRoundPrizeQueryPort = eventRoundPrizeQueryPort;
        this.prizeQueryPort = prizeQueryPort;
        this.attendanceDomainService = attendanceDomainService;
        this.attendanceProcessor = attendanceProcessor;
    }

    @Transactional
    public AttendEventTransactionResult attend(AttendEventCommand command) {
        // 1. 이벤트와 회차를 조회하고 유효한 출석 요청인지 먼저 검증한다.
        EventEntity event = eventQueryPort.findById(command.eventId())
                .orElseThrow(() -> BusinessException.from(EventCode.EVENT_NOT_FOUND));

        // 회차 존재 여부는 PK로 확인하고, 이벤트 소속 일치는 도메인 검증에서 분리해 확인한다.
        EventRoundEntity round = eventRoundQueryPort.findById(command.roundId())
                .orElseThrow(() -> BusinessException.from(EventCode.EVENT_ROUND_NOT_FOUND));

        attendanceDomainService.validateAttendable(event, round, Instant.now());

        // 2. applicant 저장으로 같은 회차 재출석을 막는다.
        EventApplicantEntity applicant = saveApplicant(command);

        // 3. 회차에 연결된 보상 매핑을 조회하고 실제 지급 가능한지 확인한다.
        List<EventRoundPrizeEntity> eventRoundPrizes = eventRoundPrizeQueryPort.findActiveByRoundId(command.roundId());
        PrizeEntity prize = resolvePrize(eventRoundPrizes);

        attendanceDomainService.validateAttendanceReward(eventRoundPrizes, prize);

        AttendanceRewardInfo rewardInfo = buildRewardInfo(eventRoundPrizes, prize);

        // 4. 응답에 필요한 누적 출석 수를 계산한다.
        int attendedDays = Math.toIntExact(eventEntryQueryPort.countByEventIdAndMemberId(
                command.eventId(),
                command.memberId()
        ) + 1);
        int totalDays = Math.toIntExact(eventRoundQueryPort.countByEventId(command.eventId()));

        // 5. entry/win 저장과 유스케이스 결과 조립은 processor에 위임한다.
        AttendEventResult attendEventResult = attendanceProcessor.process(
                applicant,
                round,
                command.memberId(),
                rewardInfo,
                attendedDays,
                totalDays
        );

        // 6. 트랜잭션 밖에서 point API를 호출할 수 있도록 명령을 함께 반환한다.
        return AttendEventTransactionResult.of(
                attendEventResult,
                buildPointGrantCommand(command, rewardInfo)
        );
    }

    private EventApplicantEntity saveApplicant(AttendEventCommand command) {
        try {
            // applicant는 "이 회원이 이 회차 출석을 시도했다"는 중복 제어용 기록이다.
            return eventApplicantCommandPort.save(EventApplicantEntity.create(
                    command.eventId(),
                    command.roundId(),
                    command.memberId(),
                    command.memberId()
            ));
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
        if (eventRoundPrizes.isEmpty() || prize == null) {
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
            AttendEventCommand command,
            AttendanceRewardInfo rewardInfo
    ) {
        // point 보상이 없으면 외부 포인트 시스템에 전달할 명령도 만들지 않는다.
        if (rewardInfo == null) {
            return null;
        }

        // idempotencyKey는 외부 API 중복 지급을 막기 위한 키다.
        return PointGrantCommand.of(
                command.eventId(),
                command.roundId(),
                command.memberId(),
                rewardInfo.eventRoundPrizeId(),
                rewardInfo.pointAmount(),
                IDEMPOTENCY_KEY_FORMAT.formatted(command.eventId(), command.roundId(), command.memberId())
        );
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
}
