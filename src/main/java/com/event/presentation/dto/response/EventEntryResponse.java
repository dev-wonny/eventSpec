package com.event.presentation.dto.response;

import com.event.application.dto.attendance.AttendEventResult;
import com.event.domain.model.RewardType;
import java.time.Instant;
import lombok.Builder;

/**
 * 이벤트 응모
 * POST https://event-api.dolfarmer.com/event/v1/events/{eventId}/rounds/{roundId}/entries
 * api 문서에 정해진 이벤트 응모 결과 dto
 * 출석체크형 이벤트 (ATTENDANCE): roundId = 해당 날짜 회차 → 출석 + 응모 + 즉시 포인트 지급 결과 포함
 */
@Builder
public record EventEntryResponse(
        Long entryId,
        Instant appliedAt,
        Integer roundNo,
        Boolean isWinner,
        WinResponse win,
        AttendanceResponse attendance
) {

    public static EventEntryResponse from(AttendEventResult result) {
        return EventEntryResponse.builder()
                .entryId(result.entryId())
                .appliedAt(result.appliedAt())
                .roundNo(result.roundNo())
                .isWinner(result.isWinner())
                .win(WinResponse.from(result))
                .attendance(AttendanceResponse.from(result))
                .build();
    }

    @Builder
    public record WinResponse(
            Long winId,
            String prizeName,
            RewardType rewardType,
            Integer pointAmount,
            String couponCode
    ) {

        public static WinResponse from(AttendEventResult result) {
            if (result.win() == null) {
                return null;
            }

            return WinResponse.builder()
                    .winId(result.win().winId())
                    .prizeName(result.win().prizeName())
                    .rewardType(result.win().rewardType())
                    .pointAmount(result.win().pointAmount())
                    .couponCode(result.win().couponCode())
                    .build();
        }
    }

    @Builder
    public record AttendanceResponse(
            Integer attendedDays,
            Integer totalDays
    ) {

        public static AttendanceResponse from(AttendEventResult result) {
            if (result.attendance() == null) {
                return null;
            }

            return AttendanceResponse.builder()
                    .attendedDays(result.attendance().attendedDays())
                    .totalDays(result.attendance().totalDays())
                    .build();
        }
    }
}
