package com.event.application.dto.condition;

import com.event.domain.model.EventType;
import lombok.Builder;

@Builder
public record EventSearchCondition(
        String eventName,
        EventType eventType,
        Boolean isActive,
        Boolean isVisible,
        Long supplierId
) implements SearchCondition {

    public static EventSearchCondition empty() {
        return EventSearchCondition.builder().build();
    }

    public static EventSearchCondition of(
            String eventName,
            EventType eventType,
            Boolean isActive,
            Boolean isVisible,
            Long supplierId
    ) {
        return EventSearchCondition.builder()
                .eventName(eventName)
                .eventType(eventType)
                .isActive(isActive)
                .isVisible(isVisible)
                .supplierId(supplierId)
                .build();
    }
}
