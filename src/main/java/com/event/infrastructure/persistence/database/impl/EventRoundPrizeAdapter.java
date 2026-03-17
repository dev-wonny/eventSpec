package com.event.infrastructure.persistence.database.impl;

import static com.event.domain.entity.QEventRoundPrizeEntity.eventRoundPrizeEntity;

import com.event.application.port.output.EventRoundPrizeQueryPort;
import com.event.domain.entity.EventRoundPrizeEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventRoundPrizeAdapter implements EventRoundPrizeQueryPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EventRoundPrizeEntity> findActiveByRoundId(Long roundId) {
        return queryFactory.selectFrom(eventRoundPrizeEntity)
                .where(
                        eventRoundPrizeEntity.roundId.eq(roundId),
                        eventRoundPrizeEntity.isActive.isTrue(),
                        eventRoundPrizeEntity.isDeleted.isFalse()
                )
                .orderBy(eventRoundPrizeEntity.priority.asc(), eventRoundPrizeEntity.id.asc())
                .fetch();
    }

    @Override
    public Map<Long, EventRoundPrizeEntity> findByIds(Collection<Long> eventRoundPrizeIds) {
        if (eventRoundPrizeIds == null || eventRoundPrizeIds.isEmpty()) {
            return Map.of();
        }

        return queryFactory.selectFrom(eventRoundPrizeEntity)
                .where(
                        eventRoundPrizeEntity.id.in(eventRoundPrizeIds)
                )
                .fetch()
                .stream()
                .collect(Collectors.toMap(EventRoundPrizeEntity::getId, Function.identity()));
    }
}
