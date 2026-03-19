package com.event.infrastructure.persistence.database.impl;

import com.event.application.port.output.EventApplicantRepositoryPort;
import com.event.domain.entity.EventApplicantEntity;
import com.event.infrastructure.persistence.database.repository.EventApplicantJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventApplicantImpl implements EventApplicantRepositoryPort {

    private final EventApplicantJpaRepository eventApplicantJpaRepository;

    @Override
    public List<EventApplicantEntity> findByEventIdAndMemberIdForUpdate(Long eventId, Long memberId) {
        return eventApplicantJpaRepository.findByEvent_IdAndMemberIdOrderByIdAsc(eventId, memberId);
    }

    @Override
    public EventApplicantEntity save(EventApplicantEntity eventApplicant) {
        return eventApplicantJpaRepository.save(eventApplicant);
    }
}
