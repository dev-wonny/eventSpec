package com.event.application.port.output;

import com.event.domain.entity.EventRoundEntity;
import java.util.List;
import java.util.Optional;

public interface EventRoundQueryPort {

    /**
     * 회차 존재 여부와 이벤트 소속 검증을 분리하기 위해 회차는 PK 기준으로 조회한다.
     * 이벤트-회차 불일치는 도메인 서비스에서 ROUND_EVENT_MISMATCH로 검증한다.
     */
    Optional<EventRoundEntity> findById(Long roundId);

    List<EventRoundEntity> findAllByEventId(Long eventId);

    long countByEventId(Long eventId);
}
