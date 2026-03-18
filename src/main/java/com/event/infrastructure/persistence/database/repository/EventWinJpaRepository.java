package com.event.infrastructure.persistence.database.repository;

import com.event.domain.entity.EventWinEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventWinJpaRepository extends JpaRepository<EventWinEntity, Long> {

    List<EventWinEntity> findByEventIdAndMemberIdAndIsDeletedFalse(Long eventId, Long memberId);
}
