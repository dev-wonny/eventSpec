package com.event.presentation.dto.response;

import com.event.application.dto.event.EventWinInfoDto;
import com.event.domain.model.RewardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "회차 당첨 정보 응답")
@Builder
public record EventRoundWinResponse(
        @Schema(description = "경품명")
        String prizeName,
        @Schema(description = "보상 유형")
        RewardType rewardType,
        @Schema(description = "지급 포인트 금액")
        Integer pointAmount
) {

    public static EventRoundWinResponse from(EventWinInfoDto dto) {
        if (dto == null) {
            return null;
        }

        return EventRoundWinResponse.builder()
                .prizeName(dto.prizeName())
                .rewardType(dto.rewardType())
                .pointAmount(dto.pointAmount())
                .build();
    }
}
