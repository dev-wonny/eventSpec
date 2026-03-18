package com.event.infrastructure.persistence.database.impl;

import com.event.application.port.output.EventApplicantQueryPort;
import com.event.domain.entity.EventApplicantEntity;
import com.event.infrastructure.persistence.database.repository.EventApplicantJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventApplicantQueryImpl implements EventApplicantQueryPort {

    private final EventApplicantJpaRepository eventApplicantJpaRepository;

    @Override
    public Optional<EventApplicantEntity> findByEventIdAndMemberId(Long eventId, Long memberId) {
        return eventApplicantJpaRepository.findByEventIdAndMemberIdAndIsDeletedFalse(eventId, memberId);
    }
}
