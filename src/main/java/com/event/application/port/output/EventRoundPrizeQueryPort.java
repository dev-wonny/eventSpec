package com.event.application.port.output;

import com.event.domain.entity.EventRoundPrizeEntity;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface EventRoundPrizeQueryPort {

    List<EventRoundPrizeEntity> findActiveByRoundId(Long roundId);

    Map<Long, EventRoundPrizeEntity> findByIds(Collection<Long> eventRoundPrizeIds);
}

