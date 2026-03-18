package com.event.application.dto.attendance.internal;

import com.event.domain.model.RewardType;
import lombok.Builder;

/**
 * 출석 보상 계산 후 Service 내부에서 잠깐 전달하는 보조 DTO.
 *
 * prize/eventRoundPrize 조회 결과를 매번 여러 파라미터로 넘기지 않고,
 * 저장과 외부 연동에 필요한 값만 묶기 위한 internal 전용 구조다.
 */
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
