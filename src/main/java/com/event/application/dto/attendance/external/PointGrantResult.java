package com.event.application.dto.attendance.external;

import lombok.Builder;

/**
 * 포인트 지급 외부 연동 결과 DTO.
 *
 * 외부 시스템 응답을 application 내부에서 바로 문자열로 흩어 쓰지 않고,
 * 현재 필요한 값만 명시적으로 받기 위해 분리했다.
 */
@Builder
public record PointGrantResult(
        String externalRequestId
) {

    public static PointGrantResult from(String externalRequestId) {
        return PointGrantResult.builder()
                .externalRequestId(externalRequestId)
                .build();
    }
}
