package com.event.infrastructure.persistence.database.impl;

import com.event.application.port.output.EventWinCommandPort;
import com.event.application.port.output.EventWinQueryPort;
import com.event.domain.entity.EventWinEntity;
import com.event.infrastructure.persistence.database.repository.EventWinJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventWinImpl implements EventWinQueryPort, EventWinCommandPort {

    private final EventWinJpaRepository eventWinJpaRepository;

    @Override
    public List<EventWinEntity> findByEventIdAndMemberId(Long eventId, Long memberId) {
        return eventWinJpaRepository.findByEvent_IdAndMemberIdAndIsDeletedFalse(eventId, memberId);
    }

    @Override
    public EventWinEntity save(EventWinEntity eventWin) {
        return eventWinJpaRepository.save(eventWin);
    }
}
