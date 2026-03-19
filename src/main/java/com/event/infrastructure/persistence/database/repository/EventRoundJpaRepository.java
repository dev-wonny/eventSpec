package com.event.infrastructure.persistence.database.repository;

import com.event.domain.entity.EventRoundEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface EventRoundJpaRepository extends JpaRepository<EventRoundEntity, Long> {

    @EntityGraph(attributePaths = "event")
    Optional<EventRoundEntity> findByIdAndIsDeletedFalse(Long roundId);

    List<EventRoundEntity> findByEvent_IdAndIsDeletedFalseOrderByRoundNoAsc(Long eventId);

    long countByEvent_IdAndIsDeletedFalse(Long eventId);
}
