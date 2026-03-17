package com.event.application.dto.attendance;

import lombok.Builder;

@Builder
public record PointGrantCommand(
        Long eventId,
        Long roundId,
        Long memberId,
        Long eventRoundPrizeId,
        Integer pointAmount,
        String idempotencyKey
) {

    public static PointGrantCommand of(
            Long eventId,
            Long roundId,
            Long memberId,
            Long eventRoundPrizeId,
            Integer pointAmount,
            String idempotencyKey
    ) {
        return PointGrantCommand.builder()
                .eventId(eventId)
                .roundId(roundId)
                .memberId(memberId)
                .eventRoundPrizeId(eventRoundPrizeId)
                .pointAmount(pointAmount)
                .idempotencyKey(idempotencyKey)
                .build();
    }
}
