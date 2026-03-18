package com.event.infrastructure.persistence.database.repository;

import com.event.domain.entity.EventRoundEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRoundJpaRepository extends JpaRepository<EventRoundEntity, Long> {

    Optional<EventRoundEntity> findByIdAndIsDeletedFalse(Long roundId);

    List<EventRoundEntity> findByEventIdAndIsDeletedFalseOrderByRoundNoAsc(Long eventId);

    long countByEventIdAndIsDeletedFalse(Long eventId);
}
