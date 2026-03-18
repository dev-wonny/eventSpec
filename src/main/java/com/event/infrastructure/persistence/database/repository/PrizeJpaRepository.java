package com.event.infrastructure.persistence.database.repository;

import com.event.domain.entity.PrizeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrizeJpaRepository extends JpaRepository<PrizeEntity, Long> {

    Optional<PrizeEntity> findByIdAndIsDeletedFalse(Long prizeId);
}
