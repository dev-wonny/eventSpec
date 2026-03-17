package com.event.application.dto.attendance;

import lombok.Builder;

@Builder
public record AttendEventCommand(
        Long eventId,
        Long roundId,
        Long memberId
) {

    public static AttendEventCommand of(Long eventId, Long roundId, Long memberId) {
        return AttendEventCommand.builder()
                .eventId(eventId)
                .roundId(roundId)
                .memberId(memberId)
                .build();
    }
}
