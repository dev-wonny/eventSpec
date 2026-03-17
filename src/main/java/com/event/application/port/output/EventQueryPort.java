package com.event.application.port.output;

import com.event.domain.entity.EventEntity;
import java.util.Optional;

public interface EventQueryPort {

    Optional<EventEntity> findById(Long eventId);
}

