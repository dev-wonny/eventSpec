package com.event.application.port.output;

import com.event.domain.entity.EventEntryEntity;
import java.util.Set;

/**
 * event_entry 저장소 접근을 담당하는 통합 포트.
 *
 * 현재 구조에서는 조회/저장을 같은 repository 추상화로 다룬다.
 */
public interface EventEntryRepositoryPort {

    Set<Long> findAttendedRoundIdsByEventIdAndMemberId(Long eventId, Long memberId);

    EventEntryEntity save(EventEntryEntity eventEntry);
}
