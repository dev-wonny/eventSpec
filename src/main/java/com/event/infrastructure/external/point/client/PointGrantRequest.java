package com.event.infrastructure.external.point.client;

import com.event.application.dto.attendance.PointGrantCommand;
import lombok.Builder;

@Builder
public record PointGrantRequest(
        Long memberId,
        Integer pointAmount,
        String idempotencyKey
) {

    public static PointGrantRequest from(PointGrantCommand command) {
        return PointGrantRequest.builder()
                .memberId(command.memberId())
                .pointAmount(command.pointAmount())
                .idempotencyKey(command.idempotencyKey())
                .build();
    }
}
