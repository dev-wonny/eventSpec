package com.event.application.dto.attendance;

import lombok.Builder;

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
