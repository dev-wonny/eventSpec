package com.event.application.service;

import com.event.application.dto.attendance.internal.AttendanceRewardInfo;
import com.event.application.dto.attendance.result.AttendEventResult;
import com.event.application.dto.attendance.result.AttendanceSummaryDto;
import com.event.application.dto.attendance.result.AttendanceWinDto;
import com.event.application.port.output.EventEntryCommandPort;
import com.event.application.port.output.EventWinCommandPort;
import com.event.domain.entity.EventApplicantEntity;
import com.event.domain.entity.EventEntryEntity;
import com.event.domain.entity.EventRoundEntity;
import com.event.domain.entity.EventWinEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointAttendanceProcessor implements AttendanceProcessor {

    private final EventEntryCommandPort eventEntryCommandPort;
    private final EventWinCommandPort eventWinCommandPort;

    @Override
    public AttendEventResult process(
            EventApplicantEntity applicant,
            EventRoundEntity round,
            Long memberId,
            AttendanceRewardInfo rewardInfo,
            int attendedDays,
            int totalDays
    ) {
        Long actor = memberId;
        EventEntryEntity eventEntry = EventEntryEntity.create(
                applicant.getId(),
                applicant.getEventId(),
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
