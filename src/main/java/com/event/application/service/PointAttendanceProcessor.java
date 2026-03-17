package com.event.application.service;

import com.event.application.dto.attendance.AttendEventResult;
import com.event.application.dto.attendance.AttendanceRewardInfo;
import com.event.application.dto.attendance.AttendanceSummaryDto;
import com.event.application.dto.attendance.AttendanceWinDto;
import com.event.application.dto.attendance.PointGrantCommand;
import com.event.application.port.output.EventEntryCommandPort;
import com.event.application.port.output.EventWinCommandPort;
import com.event.application.port.output.PointRewardPort;
import com.event.domain.entity.EventApplicantEntity;
import com.event.domain.entity.EventEntryEntity;
import com.event.domain.entity.EventRoundEntity;
import com.event.domain.entity.EventWinEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointAttendanceProcessor implements AttendanceProcessor {

    private static final String IDEMPOTENCY_KEY_FORMAT = "ATTENDANCE:%d:%d:%d";

    private final EventEntryCommandPort eventEntryCommandPort;
    private final EventWinCommandPort eventWinCommandPort;
    private final PointRewardPort pointRewardPort;

    @Override
    public AttendEventResult process(
            EventApplicantEntity applicant,
            EventRoundEntity round,
            Long memberId,
            AttendanceRewardInfo rewardInfo,
            int attendedDays,
            int totalDays
    ) {
        String actor = String.valueOf(memberId);
        EventEntryEntity eventEntry = EventEntryEntity.create(
                applicant.getId(),
                applicant.getEventId(),
                round.getId(),
                memberId,
                rewardInfo != null ? rewardInfo.eventRoundPrizeId() : null,
                rewardInfo != null,
                actor
        );
        EventEntryEntity savedEntry = eventEntryCommandPort.save(eventEntry);

        if (rewardInfo == null) {
            return AttendEventResult.of(
                    savedEntry.getId(),
                    savedEntry.getAppliedAt(),
                    round.getRoundNo(),
                    Boolean.FALSE,
                    null,
                    AttendanceSummaryDto.of(attendedDays, totalDays)
            );
        }

        pointRewardPort.grant(PointGrantCommand.of(
                applicant.getEventId(),
                round.getId(),
                memberId,
                rewardInfo.eventRoundPrizeId(),
                rewardInfo.pointAmount(),
                IDEMPOTENCY_KEY_FORMAT.formatted(applicant.getEventId(), round.getId(), memberId)
        ));

        EventWinEntity savedWin = eventWinCommandPort.save(EventWinEntity.create(
                savedEntry.getId(),
                round.getId(),
                applicant.getEventId(),
                memberId,
                rewardInfo.eventRoundPrizeId(),
                actor
        ));

        return AttendEventResult.of(
                savedEntry.getId(),
                savedEntry.getAppliedAt(),
                round.getRoundNo(),
                Boolean.TRUE,
                AttendanceWinDto.of(
                        savedWin.getId(),
                        rewardInfo.prizeName(),
                        rewardInfo.rewardType(),
                        rewardInfo.pointAmount(),
                        null
                ),
                AttendanceSummaryDto.of(attendedDays, totalDays)
        );
    }
}
