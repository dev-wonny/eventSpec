package com.event.infrastructure.persistence.database.repository;

import com.event.domain.entity.EventRoundPrizeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRoundPrizeJpaRepository extends JpaRepository<EventRoundPrizeEntity, Long> {

    List<EventRoundPrizeEntity> findByRound_IdAndIsActiveTrueAndIsDeletedFalseOrderByPriorityAscIdAsc(Long roundId);
}
