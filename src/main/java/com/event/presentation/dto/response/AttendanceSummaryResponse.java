package com.event.presentation.dto.response;

import com.event.application.dto.attendance.result.AttendanceSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import lombok.Builder;

@Schema(description = "출석 요약 응답")
@Builder
public record AttendanceSummaryResponse(
        @Schema(description = "출석 완료 일수")
        Long attendedDays,
        @Schema(description = "전체 출석 대상 일수")
        Long totalDays
) {

    public static AttendanceSummaryResponse from(AttendanceSummaryDto dto) {
        if (Objects.isNull(dto)) {
            return null;
        }

        return AttendanceSummaryResponse.builder()
                .attendedDays(dto.attendedDays())
                .totalDays(dto.totalDays())
                .build();
    }
}
