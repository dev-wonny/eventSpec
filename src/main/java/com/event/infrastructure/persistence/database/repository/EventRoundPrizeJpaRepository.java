package com.event.infrastructure.persistence.database.repository;

import com.event.domain.entity.EventRoundPrizeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRoundPrizeJpaRepository extends JpaRepository<EventRoundPrizeEntity, Long> {
}

