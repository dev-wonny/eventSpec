package com.event.application.service;

import com.event.application.dto.attendance.AttendEventCommand;
import com.event.application.dto.attendance.AttendEventResult;
import com.event.application.dto.attendance.AttendanceRewardInfo;
import com.event.application.port.input.AttendEventUseCase;
import com.event.application.port.output.EventApplicantQueryPort;
import com.event.application.port.output.EventEntryQueryPort;
import com.event.application.port.output.EventQueryPort;
import com.event.application.port.output.EventRoundPrizeQueryPort;
import com.event.application.port.output.EventRoundQueryPort;
import com.event.application.port.output.PrizeQueryPort;
import com.event.common.logging.LogContextKeys;
import com.event.domain.entity.EventApplicantEntity;
import com.event.domain.entity.EventEntity;
import com.event.domain.entity.EventRoundEntity;
import com.event.domain.entity.EventRoundPrizeEntity;
import com.event.domain.entity.PrizeEntity;
import com.event.domain.exception.BusinessException;
import com.event.domain.exception.code.EntryCode;
import com.event.domain.exception.code.EventCode;
import com.event.domain.exception.code.RewardCode;
import com.event.domain.service.AttendanceDomainService;
import com.event.infrastructure.external.point.client.PointApiFailedException;
import com.event.infrastructure.external.point.client.PointApiTimeoutException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendEventService implements AttendEventUseCase {

    private final EventQueryPort eventQueryPort;
    private final EventRoundQueryPort eventRoundQueryPort;
    private final EventApplicantQueryPort eventApplicantQueryPort;
    private final EventEntryQueryPort eventEntryQueryPort;
    private final EventRoundPrizeQueryPort eventRoundPrizeQueryPort;
    private final PrizeQueryPort prizeQueryPort;
    private final AttendanceDomainService attendanceDomainService;
    private final AttendanceProcessor attendanceProcessor;

    @Override
    @Transactional
    public AttendEventResult attend(AttendEventCommand command) {
        // 이벤트, 회차, 참여 신청자 정보를 먼저 조회해 출석 처리에 필요한 기본 데이터를 준비한다.
        EventEntity event = eventQueryPort.findById(command.eventId())
                .orElseThrow(() -> BusinessException.from(EventCode.EVENT_NOT_FOUND));

        EventRoundEntity round = eventRoundQueryPort.findByIdAndEventId(command.roundId(), command.eventId())
                .orElseThrow(() -> BusinessException.from(EventCode.EVENT_ROUND_NOT_FOUND));

        EventApplicantEntity applicant = eventApplicantQueryPort.findByEventIdAndMemberId(command.eventId(), command.memberId())
                .orElseThrow(() -> BusinessException.from(EntryCode.ENTRY_NOT_ALLOWED));

        boolean alreadyApplied = eventEntryQueryPort.existsByEventIdAndRoundIdAndMemberId(
                command.eventId(),
                command.roundId(),
                command.memberId()
        );

        // 현재 회차에 연결된 활성 보상과 실제 지급할 경품 정보를 조회한다.
        List<EventRoundPrizeEntity> eventRoundPrizes = eventRoundPrizeQueryPort.findActiveByRoundId(command.roundId());
        PrizeEntity prize = resolvePrize(eventRoundPrizes);

        // 도메인 규칙으로 출석 가능 여부와 보상 지급 가능 여부를 검증한다.
        attendanceDomainService.validateAttendable(event, round, applicant, alreadyApplied, Instant.now());
        attendanceDomainService.validateAttendanceReward(eventRoundPrizes, prize);

        // 출석 성공 시 응답에 포함할 보상 정보를 구성한다.
        AttendanceRewardInfo rewardInfo = null;
        if (!eventRoundPrizes.isEmpty() && prize != null) {
            EventRoundPrizeEntity eventRoundPrize = eventRoundPrizes.getFirst();
            rewardInfo = AttendanceRewardInfo.of(
                    eventRoundPrize.getId(),
                    prize.getId(),
                    prize.getPrizeName(),
                    prize.getRewardType(),
                    prize.getPointAmount()
            );
        }

        // 이번 출석을 포함한 누적 출석일 수와 전체 회차 수를 계산한다.
        int attendedDays = Math.toIntExact(eventEntryQueryPort.countByEventIdAndMemberId(
                command.eventId(),
                command.memberId()
        ) + 1);
        int totalDays = Math.toIntExact(eventRoundQueryPort.countByEventId(command.eventId()));

        try {
            // 실제 출석 저장, 보상 지급, 결과 생성은 processor에 위임한다.
            return attendanceProcessor.process(
                    applicant,
                    round,
                    command.memberId(),
                    rewardInfo,
                    attendedDays,
                    totalDays
            );
        } catch (PointApiTimeoutException ex) {
            log.error(
                    "commonCode={} domainCode={} {}={} {}={} {}={}",
                    RewardCode.POINT_API_TIMEOUT.getCommonCode().getCode(),
                    RewardCode.POINT_API_TIMEOUT.getCode(),
                    LogContextKeys.EVENT_ID,
                    command.eventId(),
                    LogContextKeys.ROUND_ID,
                    command.roundId(),
                    LogContextKeys.MEMBER_ID,
                    command.memberId(),
                    ex
            );
            throw BusinessException.from(RewardCode.POINT_API_TIMEOUT);
        } catch (PointApiFailedException ex) {
            log.error(
                    "commonCode={} domainCode={} {}={} {}={} {}={}",
                    RewardCode.POINT_API_FAILED.getCommonCode().getCode(),
                    RewardCode.POINT_API_FAILED.getCode(),
                    LogContextKeys.EVENT_ID,
                    command.eventId(),
                    LogContextKeys.ROUND_ID,
                    command.roundId(),
                    LogContextKeys.MEMBER_ID,
                    command.memberId(),
                    ex
            );
            throw BusinessException.from(RewardCode.POINT_API_FAILED);
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.from(EntryCode.ENTRY_ALREADY_APPLIED);
        }
    }

    private PrizeEntity resolvePrize(List<EventRoundPrizeEntity> eventRoundPrizes) {
        // 회차 보상이 없으면 지급할 경품도 없으므로 null을 반환한다.
        if (eventRoundPrizes.isEmpty()) {
            return null;
        }

        // 현재 구현은 첫 번째 활성 보상 기준으로 실제 경품 정보를 조회한다.
        return prizeQueryPort.findById(eventRoundPrizes.getFirst().getPrizeId())
                .orElse(null);
    }
}
