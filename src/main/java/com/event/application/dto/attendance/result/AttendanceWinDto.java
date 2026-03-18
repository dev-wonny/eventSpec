package com.event.application.dto.attendance.result;

import com.event.domain.model.RewardType;
import lombok.Builder;

/**
 * 출석 응모 결과의 당첨 정보 DTO.
 *
 * event_win Entity는 저장 기준 값만 가지므로,
 * 응답에 필요한 경품명/보상 유형 같은 조회 결과를 함께 담기 위해 별도 result DTO로 분리했다.
 */
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
