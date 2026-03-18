package com.event.infrastructure.persistence.database.impl;

import static com.event.domain.entity.QEventApplicantEntity.eventApplicantEntity;
import static com.event.domain.entity.QEventEntryEntity.eventEntryEntity;

import com.event.application.port.output.EventEntryCommandPort;
import com.event.application.port.output.EventEntryQueryPort;
import com.event.domain.entity.EventEntryEntity;
import com.event.infrastructure.persistence.database.repository.EventEntryJpaRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventEntryImpl implements EventEntryQueryPort, EventEntryCommandPort {

    private final EventEntryJpaRepository eventEntryJpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public long countByEventIdAndMemberId(Long eventId, Long memberId) {
        return eventEntryJpaRepository.countByEventIdAndMemberIdAndIsDeletedFalse(eventId, memberId);
    }

    @Override
    public Set<Long> findAttendedRoundIdsByEventIdAndMemberId(Long eventId, Long memberId) {
        return Set.copyOf(queryFactory
                .select(eventApplicantEntity.roundId)
                .distinct()
                .from(eventEntryEntity)
                .join(eventApplicantEntity)
                .on(eventApplicantEntity.id.eq(eventEntryEntity.applicantId))
                .where(
                        eventEntryEntity.eventId.eq(eventId),
                        eventEntryEntity.memberId.eq(memberId),
                        eventEntryEntity.isDeleted.isFalse(),
                        eventApplicantEntity.isDeleted.isFalse()
                )
                .fetch());
    }

    @Override
    public EventEntryEntity save(EventEntryEntity eventEntry) {
        return eventEntryJpaRepository.save(eventEntry);
    }
}
