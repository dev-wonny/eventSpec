package com.event.infrastructure.external.point.client;

import lombok.Builder;

@Builder
public record PointGrantResponse(
        String requestId
) {

    public static PointGrantResponse of(String requestId) {
        return PointGrantResponse.builder()
                .requestId(requestId)
                .build();
    }
}
