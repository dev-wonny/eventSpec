package com.event.application.dto.event;

import com.event.application.dto.attendance.result.AttendanceSummaryDto;
import com.event.domain.entity.EventEntity;
import com.event.domain.model.EventType;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record EventDetailDto(
        Long eventId,
        String eventName,
        EventType eventType,
        Instant startAt,
        Instant endAt,
        Long supplierId,
        String eventUrl,
        Integer priority,
        Boolean isActive,
        Boolean isVisible,
        String description,
        Integer totalCount,
        List<EventRoundDto> rounds,
        AttendanceSummaryDto attendanceSummary
) {

    public static EventDetailDto of(
            EventEntity event,
            List<EventRoundDto> rounds,
            AttendanceSummaryDto attendanceSummary
    ) {
        return fromEntity(event, rounds, attendanceSummary);
    }

    private static EventDetailDto fromEntity(
            EventEntity event,
            List<EventRoundDto> rounds,
            AttendanceSummaryDto attendanceSummary
    ) {
        return EventDetailDto.builder()
                .eventId(event.getId())
                .eventName(event.getEventName())
                .eventType(event.getEventType())
                .startAt(event.getStartAt())
                .endAt(event.getEndAt())
                .supplierId(event.getSupplierId())
                .eventUrl(event.getEventUrl())
                .priority(event.getPriority())
                .isActive(event.getIsActive())
                .isVisible(event.getIsVisible())
                .description(event.getDescription())
                .totalCount(rounds.size())
                .rounds(rounds)
                .attendanceSummary(attendanceSummary)
                .build();
    }
}
