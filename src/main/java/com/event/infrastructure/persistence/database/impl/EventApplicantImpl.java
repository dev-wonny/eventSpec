package com.event.infrastructure.persistence.database.impl;

import com.event.application.port.output.EventApplicantCommandPort;
import com.event.domain.entity.EventApplicantEntity;
import com.event.infrastructure.persistence.database.repository.EventApplicantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventApplicantImpl implements EventApplicantCommandPort {

    private final EventApplicantJpaRepository eventApplicantJpaRepository;

    @Override
    public EventApplicantEntity save(EventApplicantEntity eventApplicant) {
        return eventApplicantJpaRepository.save(eventApplicant);
    }
}
