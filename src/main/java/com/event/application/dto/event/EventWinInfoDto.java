package com.event.application.dto.event;

import com.event.domain.model.RewardType;
import lombok.Builder;

@Builder
public record EventWinInfoDto(
        String prizeName,
        RewardType rewardType,
        Integer pointAmount
) {

    public static EventWinInfoDto of(String prizeName, RewardType rewardType, Integer pointAmount) {
        return EventWinInfoDto.builder()
                .prizeName(prizeName)
                .rewardType(rewardType)
                .pointAmount(pointAmount)
                .build();
    }
}
