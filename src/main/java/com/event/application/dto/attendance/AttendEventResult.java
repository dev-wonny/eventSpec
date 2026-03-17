package com.event.application.dto.attendance;

import java.time.Instant;
import lombok.Builder;

@Builder
public record AttendEventResult(
        Long entryId,
        Instant appliedAt,
        Integer roundNo,
        Boolean isWinner,
        AttendanceWinDto win,
        AttendanceSummaryDto attendance
) {

    public static AttendEventResult of(
            Long entryId,
            Instant appliedAt,
            Integer roundNo,
            Boolean isWinner,
            AttendanceWinDto win,
            AttendanceSummaryDto attendance
    ) {
        return AttendEventResult.builder()
                .entryId(entryId)
                .appliedAt(appliedAt)
                .roundNo(roundNo)
                .isWinner(isWinner)
                .win(win)
                .attendance(attendance)
                .build();
    }
}
