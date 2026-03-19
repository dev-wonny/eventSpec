package com.event.infrastructure.persistence.database.repository;

import com.event.domain.entity.EventEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventEntryJpaRepository extends JpaRepository<EventEntryEntity, Long> {
}
