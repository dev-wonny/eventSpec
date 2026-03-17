package com.event.infrastructure.persistence.database.impl;

import static com.event.domain.entity.QEventEntity.eventEntity;

import com.event.application.port.output.EventQueryPort;
import com.event.domain.entity.EventEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventQueryAdapter implements EventQueryPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<EventEntity> findById(Long eventId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(eventEntity)
                        .where(
                                eventEntity.id.eq(eventId),
                                eventEntity.isDeleted.isFalse()
                        )
                        .fetchOne()
        );
    }
}

