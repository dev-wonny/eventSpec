package com.event.infrastructure.persistence.database.impl;

import com.event.application.port.output.EventRoundQueryPort;
import com.event.domain.entity.EventRoundEntity;
import com.event.infrastructure.persistence.database.repository.EventRoundJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventRoundQueryImpl implements EventRoundQueryPort {

    private final EventRoundJpaRepository eventRoundJpaRepository;

    @Override
    public Optional<EventRoundEntity> findById(Long roundId) {
        return eventRoundJpaRepository.findByIdAndIsDeletedFalse(roundId);
    }

    @Override
    public List<EventRoundEntity> findAllByEventId(Long eventId) {
        return eventRoundJpaRepository.findByEventIdAndIsDeletedFalseOrderByRoundNoAsc(eventId);
    }

    @Override
    public long countByEventId(Long eventId) {
        return eventRoundJpaRepository.countByEventIdAndIsDeletedFalse(eventId);
    }
}
