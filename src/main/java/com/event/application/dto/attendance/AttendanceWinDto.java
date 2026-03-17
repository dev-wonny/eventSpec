package com.event.application.dto.attendance;

import com.event.domain.model.RewardType;
import lombok.Builder;

@Builder
public record AttendanceWinDto(
        Long winId,
        String prizeName,
        RewardType rewardType,
        Integer pointAmount,
        String couponCode
) {

    public static AttendanceWinDto of(
            Long winId,
            String prizeName,
            RewardType rewardType,
            Integer pointAmount,
            String couponCode
    ) {
        return AttendanceWinDto.builder()
                .winId(winId)
                .prizeName(prizeName)
                .rewardType(rewardType)
                .pointAmount(pointAmount)
                .couponCode(couponCode)
                .build();
    }
}
