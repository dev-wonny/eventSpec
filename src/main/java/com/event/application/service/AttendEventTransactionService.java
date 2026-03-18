package com.event.application.service;

import com.event.application.dto.attendance.AttendEventCommand;
import com.event.application.dto.attendance.AttendEventResult;
import com.event.application.dto.attendance.AttendEventTransactionResult;
import com.event.application.dto.attendance.AttendanceRewardInfo;
import com.event.application.dto.attendance.PointGrantCommand;
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
        EventEntity event = eventQueryPort.findById(command.eventId())
                .orElseThrow(() -> BusinessException.from(EventCode.EVENT_NOT_FOUND));

        EventRoundEntity round = eventRoundQueryPort.findByIdAndEventId(command.roundId(), command.eventId())
                .orElseThrow(() -> BusinessException.from(EventCode.EVENT_ROUND_NOT_FOUND));

        attendanceDomainService.validateAttendable(event, round, Instant.now());

        EventApplicantEntity applicant = saveApplicant(command);

        List<EventRoundPrizeEntity> eventRoundPrizes = eventRoundPrizeQueryPort.findActiveByRoundId(command.roundId());
        PrizeEntity prize = resolvePrize(eventRoundPrizes);

        attendanceDomainService.validateAttendanceReward(eventRoundPrizes, prize);

        AttendanceRewardInfo rewardInfo = buildRewardInfo(eventRoundPrizes, prize);

        int attendedDays = Math.toIntExact(eventEntryQueryPort.countByEventIdAndMemberId(
                command.eventId(),
                command.memberId()
        ) + 1);
        int totalDays = Math.toIntExact(eventRoundQueryPort.countByEventId(command.eventId()));

        AttendEventResult attendEventResult = attendanceProcessor.process(
                applicant,
                round,
                command.memberId(),
                rewardInfo,
                attendedDays,
                totalDays
        );

        return AttendEventTransactionResult.of(
                attendEventResult,
                buildPointGrantCommand(command, rewardInfo)
        );
    }

    private EventApplicantEntity saveApplicant(AttendEventCommand command) {
        try {
            return eventApplicantCommandPort.save(EventApplicantEntity.create(
                    command.eventId(),
                    command.roundId(),
                    command.memberId(),
                    command.memberId()
            ));
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.from(EntryCode.ENTRY_ALREADY_APPLIED);
        }
    }

    private AttendanceRewardInfo buildRewardInfo(
            List<EventRoundPrizeEntity> eventRoundPrizes,
            PrizeEntity prize
    ) {
        if (eventRoundPrizes.isEmpty() || prize == null) {
            return null;
        }

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
        if (rewardInfo == null) {
            return null;
        }

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
        if (eventRoundPrizes.isEmpty()) {
            return null;
        }

        return prizeQueryPort.findById(eventRoundPrizes.getFirst().getPrizeId())
                .orElse(null);
    }
}
