package com.event.application.port.output;

import com.event.domain.entity.EventApplicantEntity;

public interface EventApplicantCommandPort {

    EventApplicantEntity save(EventApplicantEntity eventApplicant);
}
