package com.event.infrastructure.persistence.database.repository;

import com.event.domain.entity.EventApplicantEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface EventApplicantJpaRepository extends JpaRepository<EventApplicantEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<EventApplicantEntity> findByEvent_IdAndMemberIdOrderByIdAsc(Long eventId, Long memberId);
}
