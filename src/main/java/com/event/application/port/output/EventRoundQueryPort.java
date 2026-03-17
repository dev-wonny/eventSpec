package com.event.application.port.output;

import com.event.domain.entity.EventRoundEntity;
import java.util.List;
import java.util.Optional;

public interface EventRoundQueryPort {

    Optional<EventRoundEntity> findByIdAndEventId(Long roundId, Long eventId);

    List<EventRoundEntity> findAllByEventId(Long eventId);

    long countByEventId(Long eventId);
}

