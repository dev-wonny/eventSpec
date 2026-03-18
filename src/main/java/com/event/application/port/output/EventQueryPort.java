package com.event.application.port.output;

import com.event.application.dto.condition.EventSearchCondition;
import com.event.domain.entity.EventEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventQueryPort {

    Optional<EventEntity> findById(Long eventId);

    Page<EventEntity> findAll(Pageable pageable, EventSearchCondition condition);
}
