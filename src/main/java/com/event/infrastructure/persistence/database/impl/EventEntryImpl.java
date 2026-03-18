package com.event.infrastructure.persistence.database.impl;

import com.event.application.port.output.EventEntryCommandPort;
import com.event.application.port.output.EventEntryQueryPort;
import com.event.domain.entity.EventEntryEntity;
import com.event.infrastructure.persistence.database.repository.EventEntryJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventEntryImpl implements EventEntryQueryPort, EventEntryCommandPort {

    private final EventEntryJpaRepository eventEntryJpaRepository;

    @Override
    public boolean existsByEventIdAndRoundIdAndMemberId(Long eventId, Long roundId, Long memberId) {
        return eventEntryJpaRepository.existsByEventIdAndRoundIdAndMemberIdAndIsDeletedFalse(eventId, roundId, memberId);
    }

    @Override
    public long countByEventIdAndMemberId(Long eventId, Long memberId) {
        return eventEntryJpaRepository.countByEventIdAndMemberIdAndIsDeletedFalse(eventId, memberId);
    }

    @Override
    public List<EventEntryEntity> findByEventIdAndMemberId(Long eventId, Long memberId) {
        return eventEntryJpaRepository.findByEventIdAndMemberIdAndIsDeletedFalse(eventId, memberId);
    }

    @Override
    public EventEntryEntity save(EventEntryEntity eventEntry) {
        return eventEntryJpaRepository.save(eventEntry);
    }
}
