package com.event.infrastructure.persistence.database.impl;

import static com.event.domain.entity.QEventEntryEntity.eventEntryEntity;

import com.event.application.port.output.EventEntryCommandPort;
import com.event.application.port.output.EventEntryQueryPort;
import com.event.domain.entity.EventEntryEntity;
import com.event.infrastructure.persistence.database.repository.EventEntryJpaRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventEntryAdapter implements EventEntryQueryPort, EventEntryCommandPort {

    private final JPAQueryFactory queryFactory;
    private final EventEntryJpaRepository eventEntryJpaRepository;

    @Override
    public boolean existsByEventIdAndRoundIdAndMemberId(Long eventId, Long roundId, Long memberId) {
        Integer fetchOne = queryFactory.selectOne()
                .from(eventEntryEntity)
                .where(
                        eventEntryEntity.eventId.eq(eventId),
                        eventEntryEntity.roundId.eq(roundId),
                        eventEntryEntity.memberId.eq(memberId),
                        eventEntryEntity.isDeleted.isFalse()
                )
                .fetchFirst();
        return fetchOne != null;
    }

    @Override
    public long countByEventIdAndMemberId(Long eventId, Long memberId) {
        Long count = queryFactory.select(eventEntryEntity.count())
                .from(eventEntryEntity)
                .where(
                        eventEntryEntity.eventId.eq(eventId),
                        eventEntryEntity.memberId.eq(memberId),
                        eventEntryEntity.isDeleted.isFalse()
                )
                .fetchOne();
        return count == null ? 0L : count;
    }

    @Override
    public List<EventEntryEntity> findByEventIdAndMemberId(Long eventId, Long memberId) {
        return queryFactory.selectFrom(eventEntryEntity)
                .where(
                        eventEntryEntity.eventId.eq(eventId),
                        eventEntryEntity.memberId.eq(memberId),
                        eventEntryEntity.isDeleted.isFalse()
                )
                .fetch();
    }

    @Override
    public EventEntryEntity save(EventEntryEntity eventEntry) {
        return eventEntryJpaRepository.save(eventEntry);
    }
}

