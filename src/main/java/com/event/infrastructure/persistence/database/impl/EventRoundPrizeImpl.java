package com.event.infrastructure.persistence.database.impl;

import com.event.application.port.output.EventRoundPrizeQueryPort;
import com.event.domain.entity.EventRoundPrizeEntity;
import com.event.infrastructure.persistence.database.repository.EventRoundPrizeJpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventRoundPrizeImpl implements EventRoundPrizeQueryPort {

    private final EventRoundPrizeJpaRepository eventRoundPrizeJpaRepository;

    @Override
    public List<EventRoundPrizeEntity> findActiveByRoundId(Long roundId) {
        return eventRoundPrizeJpaRepository.findByRound_IdAndIsActiveTrueAndIsDeletedFalseOrderByPriorityAscIdAsc(roundId);
    }

    // todo : List 변경 -> Set 써야해서 안 변경해야함
    @Override
    public Map<Long, EventRoundPrizeEntity> findByIds(Collection<Long> eventRoundPrizeIds) {
        if (Objects.isNull(eventRoundPrizeIds) || eventRoundPrizeIds.isEmpty()) {
            return Map.of();
        }

        return eventRoundPrizeJpaRepository.findAllById(eventRoundPrizeIds)
                .stream()
                .collect(Collectors.toMap(EventRoundPrizeEntity::getId, Function.identity()));
    }
}
