package com.event.application.port.output;

import com.event.domain.entity.EventApplicantEntity;
import java.util.Optional;

public interface EventApplicantQueryPort {

    Optional<EventApplicantEntity> findByEventIdAndMemberId(Long eventId, Long memberId);
}

