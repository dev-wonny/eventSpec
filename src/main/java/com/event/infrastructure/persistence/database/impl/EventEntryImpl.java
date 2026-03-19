package com.event.infrastructure.persistence.database.impl;

import static com.event.domain.entity.QEventApplicantEntity.eventApplicantEntity;
import static com.event.domain.entity.QEventEntity.eventEntity;
import static com.event.domain.entity.QEventEntryEntity.eventEntryEntity;

import com.event.application.port.output.EventEntryRepositoryPort;
import com.event.application.dto.condition.EventSearchCondition;
import com.event.domain.entity.EventEntryEntity;
import com.event.infrastructure.persistence.database.builder.EventEntityBuilder;
import com.event.infrastructure.persistence.database.repository.EventEntryJpaRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventEntryImpl implements EventEntryRepositoryPort {

    private final EventEntryJpaRepository eventEntryJpaRepository;
    private final EventEntityBuilder eventEntityBuilder;
    private final JPAQueryFactory queryFactory;

    @Override
    public Set<Long> findAttendedRoundIdsByEventIdAndMemberId(Long eventId, Long memberId) {
        BooleanBuilder where = eventEntityBuilder.buildWhere(EventSearchCondition.empty())
                .and(eventEntity.id.eq(eventId))
                .and(eventApplicantEntity.memberId.eq(memberId))
                .and(eventApplicantEntity.isDeleted.isFalse())
                .and(JPAExpressions.selectOne()
                        .from(eventEntryEntity)
                        .where(
                                eventEntryEntity.applicantId.eq(eventApplicantEntity.id),
                                eventEntryEntity.event.id.eq(eventId),
                                eventEntryEntity.memberId.eq(memberId),
                                eventEntryEntity.isDeleted.isFalse()
                        )
                        .exists());

        return Set.copyOf(queryFactory
                .select(eventApplicantEntity.round.id)
                .distinct()
                .from(eventApplicantEntity)
                .join(eventApplicantEntity.event, eventEntity)
                .where(where)
                .fetch());
    }

    @Override
    public EventEntryEntity save(EventEntryEntity eventEntry) {
        return eventEntryJpaRepository.save(eventEntry);
    }
}
