package com.event.application.dto.attendance;

import com.event.domain.model.RewardType;
import lombok.Builder;

@Builder
public record AttendanceRewardInfo(
        Long eventRoundPrizeId,
        Long prizeId,
        String prizeName,
        RewardType rewardType,
        Integer pointAmount
) {

    public static AttendanceRewardInfo of(
            Long eventRoundPrizeId,
            Long prizeId,
            String prizeName,
            RewardType rewardType,
            Integer pointAmount
    ) {
        return AttendanceRewardInfo.builder()
                .eventRoundPrizeId(eventRoundPrizeId)
                .prizeId(prizeId)
                .prizeName(prizeName)
                .rewardType(rewardType)
                .pointAmount(pointAmount)
                .build();
    }
}
