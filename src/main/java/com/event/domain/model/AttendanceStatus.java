package com.event.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceStatus {
    ATTENDED("출석 완료"),
    MISSED("미출석"),
    TODAY("오늘"),
    FUTURE("예정");

    private final String label;
}
