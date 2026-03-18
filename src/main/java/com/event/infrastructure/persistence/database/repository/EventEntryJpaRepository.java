package com.event.infrastructure.persistence.database.repository;

import com.event.domain.entity.EventEntryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventEntryJpaRepository extends JpaRepository<EventEntryEntity, Long> {

    boolean existsByEventIdAndRoundIdAndMemberIdAndIsDeletedFalse(Long eventId, Long roundId, Long memberId);

    long countByEventIdAndMemberIdAndIsDeletedFalse(Long eventId, Long memberId);

    List<EventEntryEntity> findByEventIdAndMemberIdAndIsDeletedFalse(Long eventId, Long memberId);
}
