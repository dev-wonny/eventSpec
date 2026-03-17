package com.event.infrastructure.persistence.database.repository;

import com.event.domain.entity.EventApplicantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventApplicantJpaRepository extends JpaRepository<EventApplicantEntity, Long> {
}

