package com.event.infrastructure.persistence.database.repository;

import com.event.domain.entity.PrizeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrizeJpaRepository extends JpaRepository<PrizeEntity, Long> {
}

