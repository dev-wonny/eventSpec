package com.event.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {
    ATTENDANCE("출석 이벤트"),
    RANDOM_REWARD("랜덤 보상 이벤트");

    private final String label;
}
