package com.event.infrastructure.persistence.database.repository;

import com.event.domain.entity.EventApplicantEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventApplicantJpaRepository extends JpaRepository<EventApplicantEntity, Long> {

    Optional<EventApplicantEntity> findByEventIdAndMemberIdAndIsDeletedFalse(Long eventId, Long memberId);
}
