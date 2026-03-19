package com.event.application.dto.attendance.result;

import lombok.Builder;

/**
 * 출석 유스케이스에서 공통으로 사용하는 출석 요약 DTO.
 *
 * Entity 컬럼 복사본이 아니라 "현재 몇 일 출석했고 전체 대상이 몇 일인지"라는
 * 결과 의미를 재사용하기 위해 result 영역에 둔다.
 */
@Builder
public record AttendanceSummaryDto(
        long attendedDays,
        long totalDays
) {

    public static AttendanceSummaryDto of(long attendedDays, long totalDays) {
        return AttendanceSummaryDto.builder()
                .attendedDays(attendedDays)
                .totalDays(totalDays)
                .build();
    }
}
