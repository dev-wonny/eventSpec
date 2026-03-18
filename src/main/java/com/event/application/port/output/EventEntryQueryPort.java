package com.event.application.port.output;

import java.util.Set;

public interface EventEntryQueryPort {

    long countByEventIdAndMemberId(Long eventId, Long memberId);

    Set<Long> findAttendedRoundIdsByEventIdAndMemberId(Long eventId, Long memberId);
}
