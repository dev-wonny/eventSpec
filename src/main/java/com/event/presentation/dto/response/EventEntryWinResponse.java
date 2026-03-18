package com.event.presentation.dto.response;

import com.event.application.dto.attendance.result.AttendanceWinDto;
import com.event.domain.model.RewardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "응모 당첨 정보 응답")
@Builder
public record EventEntryWinResponse(
        @Schema(description = "당첨 ID")
        Long winId,
        @Schema(description = "경품명")
        String prizeName,
        @Schema(description = "보상 유형")
        RewardType rewardType,
        @Schema(description = "지급 포인트 금액")
        Integer pointAmount,
        @Schema(description = "쿠폰 코드. 쿠폰 보상이 아니면 null")
        String couponCode
) {

    public static EventEntryWinResponse from(AttendanceWinDto dto) {
        if (dto == null) {
            return null;
        }

        return EventEntryWinResponse.builder()
                .winId(dto.winId())
                .prizeName(dto.prizeName())
                .rewardType(dto.rewardType())
                .pointAmount(dto.pointAmount())
                .couponCode(dto.couponCode())
                .build();
    }
}
