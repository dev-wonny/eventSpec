package com.event.infrastructure.persistence.database.impl;

import static com.event.domain.entity.QPrizeEntity.prizeEntity;

import com.event.application.port.output.PrizeQueryPort;
import com.event.domain.entity.PrizeEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PrizeQueryAdapter implements PrizeQueryPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<PrizeEntity> findById(Long prizeId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(prizeEntity)
                        .where(
                                prizeEntity.id.eq(prizeId),
                                prizeEntity.isDeleted.isFalse()
                        )
                        .fetchOne()
        );
    }

    @Override
    public Map<Long, PrizeEntity> findByIds(Collection<Long> prizeIds) {
        if (prizeIds == null || prizeIds.isEmpty()) {
            return Map.of();
        }

        return queryFactory.selectFrom(prizeEntity)
                .where(
                        prizeEntity.id.in(prizeIds)
                )
                .fetch()
                .stream()
                .collect(Collectors.toMap(PrizeEntity::getId, Function.identity()));
    }
}
