package com.event.infrastructure.persistence.database.impl;

import com.event.application.port.output.PrizeQueryPort;
import com.event.domain.entity.PrizeEntity;
import com.event.infrastructure.persistence.database.repository.PrizeJpaRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PrizeQueryImpl implements PrizeQueryPort {

    private final PrizeJpaRepository prizeJpaRepository;

    @Override
    public Optional<PrizeEntity> findById(Long prizeId) {
        return prizeJpaRepository.findByIdAndIsDeletedFalse(prizeId);
    }

    // todo : List 변경 -> Set 써야해서 안 변경해야함
    @Override
    public Map<Long, PrizeEntity> findByIds(Collection<Long> prizeIds) {
        if (Objects.isNull(prizeIds) || prizeIds.isEmpty()) {
            return Map.of();
        }

        return prizeJpaRepository.findAllById(prizeIds)
                .stream()
                .collect(Collectors.toMap(PrizeEntity::getId, Function.identity()));
    }
}
