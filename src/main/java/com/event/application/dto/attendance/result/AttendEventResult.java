package com.event.application.dto.attendance.result;

import java.time.Instant;
import lombok.Builder;

/**
 * 출석 응모 유스케이스 결과 DTO.
 *
 * 저장용 Entity 구조를 그대로 노출하지 않고,
 * 호출자가 알아야 할 결과(entryId, roundNo, 당첨 정보, 출석 요약)를 기준으로 표현하기 위해 분리했다.
 * Controller는 이 값을 바로 JSON으로 내보내지 않고 response DTO로 한 번 더 변환한다.
 */
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
