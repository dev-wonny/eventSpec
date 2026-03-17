package com.event.presentation.dto.response;

import com.event.application.dto.attendance.AttendEventResult;
import com.event.domain.model.RewardType;
import java.time.Instant;
import lombok.Builder;

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
