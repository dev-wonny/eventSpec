package com.event.application.dto.attendance;

import lombok.Builder;

@Builder
public record AttendanceSummaryDto(
        int attendedDays,
        int totalDays
) {

    public static AttendanceSummaryDto of(int attendedDays, int totalDays) {
        return AttendanceSummaryDto.builder()
                .attendedDays(attendedDays)
                .totalDays(totalDays)
                .build();
    }
}
