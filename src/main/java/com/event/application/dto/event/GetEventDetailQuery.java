package com.event.application.dto.event;

import lombok.Builder;

@Builder
public record GetEventDetailQuery(
        Long eventId,
        Long memberId
) {

    public static GetEventDetailQuery of(Long eventId, Long memberId) {
        return GetEventDetailQuery.builder()
                .eventId(eventId)
                .memberId(memberId)
                .build();
    }
}
