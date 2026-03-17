package com.event.infrastructure.persistence.database.impl;

import static com.event.domain.entity.QEventApplicantEntity.eventApplicantEntity;

import com.event.application.port.output.EventApplicantQueryPort;
import com.event.domain.entity.EventApplicantEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventApplicantQueryAdapter implements EventApplicantQueryPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<EventApplicantEntity> findByEventIdAndMemberId(Long eventId, Long memberId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(eventApplicantEntity)
                        .where(
                                eventApplicantEntity.eventId.eq(eventId),
                                eventApplicantEntity.memberId.eq(memberId),
                                eventApplicantEntity.isDeleted.isFalse()
                        )
                        .fetchOne()
        );
    }
}

