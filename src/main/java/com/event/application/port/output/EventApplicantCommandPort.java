package com.event.application.port.output;

import com.event.domain.entity.EventApplicantEntity;

/**
 * event_applicant 저장 포트.
 *
 * 현재 구조에서는 output port를 repository 추상화에 가깝게 사용하므로,
 * Application Service가 생성한 domain Entity를 그대로 받아 저장한다.
 */
public interface EventApplicantCommandPort {

    EventApplicantEntity save(EventApplicantEntity eventApplicant);
}
