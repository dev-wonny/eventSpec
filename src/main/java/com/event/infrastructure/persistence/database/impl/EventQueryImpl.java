package com.event.infrastructure.persistence.database.impl;

import static com.event.domain.entity.QEventEntity.eventEntity;

import com.event.application.dto.condition.EventSearchCondition;
import com.event.application.port.output.EventQueryPort;
import com.event.domain.entity.EventEntity;
import com.event.infrastructure.persistence.database.builder.EventEntityBuilder;
import com.event.infrastructure.persistence.database.repository.EventJpaRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EventQueryImpl implements EventQueryPort {

    private final EventJpaRepository eventJpaRepository;
    private final EventEntityBuilder eventEntityBuilder;
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<EventEntity> findById(Long eventId) {
        return eventJpaRepository.findById(eventId);
    }

    @Override
    public Page<EventEntity> findAll(Pageable pageable, EventSearchCondition condition) {
        BooleanBuilder where = eventEntityBuilder.buildWhere(condition);

        List<EventEntity> content = queryFactory
                .selectFrom(eventEntity)
                .where(where)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(eventEntity.priority.asc(), eventEntity.createdAt.desc())
                .fetch();

        Long total = queryFactory
                .select(eventEntity.count())
                .from(eventEntity)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, Objects.nonNull(total) ? total : 0L);
    }
}
