package com.event.infrastructure.persistence.database.impl;

import static com.event.domain.entity.QEventRoundEntity.eventRoundEntity;

import com.event.application.port.output.EventRoundQueryPort;
import com.event.domain.entity.EventRoundEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventRoundQueryAdapter implements EventRoundQueryPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<EventRoundEntity> findByIdAndEventId(Long roundId, Long eventId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(eventRoundEntity)
                        .where(
                                eventRoundEntity.id.eq(roundId),
                                eventRoundEntity.eventId.eq(eventId),
                                eventRoundEntity.isDeleted.isFalse()
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<EventRoundEntity> findAllByEventId(Long eventId) {
        return queryFactory.selectFrom(eventRoundEntity)
                .where(
                        eventRoundEntity.eventId.eq(eventId),
                        eventRoundEntity.isDeleted.isFalse()
                )
                .orderBy(eventRoundEntity.roundNo.asc())
                .fetch();
    }

    @Override
    public long countByEventId(Long eventId) {
        Long count = queryFactory.select(eventRoundEntity.count())
                .from(eventRoundEntity)
                .where(
                        eventRoundEntity.eventId.eq(eventId),
                        eventRoundEntity.isDeleted.isFalse()
                )
                .fetchOne();
        return count == null ? 0L : count;
    }
}

