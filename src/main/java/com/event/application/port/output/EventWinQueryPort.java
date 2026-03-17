package com.event.application.port.output;

import com.event.domain.entity.EventWinEntity;
import java.util.List;

public interface EventWinQueryPort {

    List<EventWinEntity> findByEventIdAndMemberId(Long eventId, Long memberId);
}

