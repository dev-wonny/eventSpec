package com.event.application.port.output;

import com.event.domain.entity.EventEntryEntity;

/**
 * event_entry 저장 포트.
 *
 * 현재 구조에서는 저장 경계에서 별도 DTO를 두지 않고 domain Entity를 그대로 사용한다.
 */
public interface EventEntryCommandPort {

    EventEntryEntity save(EventEntryEntity eventEntry);
}
