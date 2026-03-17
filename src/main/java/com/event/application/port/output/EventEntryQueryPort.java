package com.event.application.port.output;

import com.event.domain.entity.EventEntryEntity;
import java.util.List;

public interface EventEntryQueryPort {

    boolean existsByEventIdAndRoundIdAndMemberId(Long eventId, Long roundId, Long memberId);

    long countByEventIdAndMemberId(Long eventId, Long memberId);

    List<EventEntryEntity> findByEventIdAndMemberId(Long eventId, Long memberId);
}

