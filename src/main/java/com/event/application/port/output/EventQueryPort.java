package com.event.application.port.output;

import com.event.application.dto.condition.EventSearchCondition;
import com.event.domain.entity.EventEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 이벤트 조회 포트.
 *
 * 현재 조회 포트는 도메인 판단과 후속 조립에 바로 사용할 수 있도록 Entity를 반환한다.
 */
public interface EventQueryPort {

    Optional<EventEntity> findById(Long eventId);

    Page<EventEntity> findAll(Pageable pageable, EventSearchCondition condition);
}
