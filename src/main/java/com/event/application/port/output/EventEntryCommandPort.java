package com.event.application.port.output;

import com.event.domain.entity.EventEntryEntity;

public interface EventEntryCommandPort {

    EventEntryEntity save(EventEntryEntity eventEntry);
}

