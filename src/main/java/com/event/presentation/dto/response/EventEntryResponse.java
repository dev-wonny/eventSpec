package com.event.presentation.dto.response;

import com.event.application.dto.attendance.result.AttendEventResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;

/**
 * 이벤트 응모
 * POST https://event-api.dolfarmer.com/event/v1/events/{eventId}/rounds/{roundId}/entries
 * api 문서에 정해진 이벤트 응모 결과 dto
 * 출석체크형 이벤트 (ATTENDANCE): roundId = 해당 날짜 회차 → 출석 + 응모 + 즉시 포인트 지급 결과 포함
 *
 * application result를 외부 응답 계약에 맞게 변환하는 presentation DTO다.
 */
@Schema(description = "출석 이벤트 응모 응답")
@Builder
public record EventEntryResponse(
        @Schema(description = "응모 ID")
        Long entryId,
        @Schema(description = "응모 시각")
        Instant appliedAt,
        @Schema(description = "응모한 회차 번호")
        Integer roundNo,
        @Schema(description = "당첨 여부")
        Boolean isWinner,
        @Schema(description = "당첨 정보. 미당첨 시 null")
        EventEntryWinResponse win,
        @Schema(description = "출석 현황 요약")
        AttendanceSummaryResponse attendance
) {

    public static EventEntryResponse from(AttendEventResult result) {
        return EventEntryResponse.builder()
                .entryId(result.entryId())
                .appliedAt(result.appliedAt())
                .roundNo(result.roundNo())
                .isWinner(result.isWinner())
                .win(EventEntryWinResponse.from(result.win()))
                .attendance(AttendanceSummaryResponse.from(result.attendance()))
                .build();
    }
}
