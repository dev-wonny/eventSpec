package com.event.application.port.output;

import com.event.domain.entity.EventApplicantEntity;
import java.util.List;

/**
 * event_applicant 저장소 접근 포트.
 *
 * 회차별 applicant 저장과 누적 출석 수 계산에 필요한 집합 잠금 조회를 함께 담당한다.
 */
public interface EventApplicantRepositoryPort {

    List<EventApplicantEntity> findByEventIdAndMemberIdForUpdate(Long eventId, Long memberId);

    EventApplicantEntity save(EventApplicantEntity eventApplicant);
}
