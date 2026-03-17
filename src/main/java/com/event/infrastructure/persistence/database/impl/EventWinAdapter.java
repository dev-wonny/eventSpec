package com.event.infrastructure.persistence.database.impl;

import static com.event.domain.entity.QEventWinEntity.eventWinEntity;

import com.event.application.port.output.EventWinCommandPort;
import com.event.application.port.output.EventWinQueryPort;
import com.event.domain.entity.EventWinEntity;
import com.event.infrastructure.persistence.database.repository.EventWinJpaRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventWinAdapter implements EventWinQueryPort, EventWinCommandPort {

    private final JPAQueryFactory queryFactory;
    private final EventWinJpaRepository eventWinJpaRepository;

    @Override
    public List<EventWinEntity> findByEventIdAndMemberId(Long eventId, Long memberId) {
        return queryFactory.selectFrom(eventWinEntity)
                .where(
                        eventWinEntity.eventId.eq(eventId),
                        eventWinEntity.memberId.eq(memberId),
                        eventWinEntity.isDeleted.isFalse()
                )
                .fetch();
    }

    @Override
    public EventWinEntity save(EventWinEntity eventWin) {
        return eventWinJpaRepository.save(eventWin);
    }
}

