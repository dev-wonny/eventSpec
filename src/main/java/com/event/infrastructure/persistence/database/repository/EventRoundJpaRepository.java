package com.event.infrastructure.persistence.database.repository;

import com.event.domain.entity.EventRoundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRoundJpaRepository extends JpaRepository<EventRoundEntity, Long> {
}

