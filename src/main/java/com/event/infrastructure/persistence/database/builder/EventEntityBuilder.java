package com.event.infrastructure.persistence.database.builder;

import static com.event.domain.entity.QEventEntity.eventEntity;

import com.event.application.dto.condition.EventSearchCondition;
import com.event.domain.model.EventType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EventEntityBuilder {

    public BooleanBuilder buildWhere(EventSearchCondition condition) {
        EventSearchCondition safeCondition = condition == null ? EventSearchCondition.empty() : condition;

        return new BooleanBuilder()
                .and(eventEntity.isDeleted.isFalse())
                .and(eventNameContains(safeCondition.eventName()))
                .and(eventTypeEq(safeCondition.eventType()))
                .and(isActiveEq(safeCondition.isActive()))
                .and(isVisibleEq(safeCondition.isVisible()))
                .and(supplierIdEq(safeCondition.supplierId()));
    }

    private BooleanExpression eventNameContains(String eventName) {
        return StringUtils.hasText(eventName)
                ? eventEntity.eventName.containsIgnoreCase(eventName)
                : null;
    }

    private BooleanExpression eventTypeEq(EventType eventType) {
        return Objects.nonNull(eventType)
                ? eventEntity.eventType.eq(eventType)
                : null;
    }

    private BooleanExpression isActiveEq(Boolean isActive) {
        return Objects.nonNull(isActive)
                ? eventEntity.isActive.eq(isActive)
                : null;
    }

    private BooleanExpression isVisibleEq(Boolean isVisible) {
        return Objects.nonNull(isVisible)
                ? eventEntity.isVisible.eq(isVisible)
                : null;
    }

    private BooleanExpression supplierIdEq(Long supplierId) {
        return Objects.nonNull(supplierId)
                ? eventEntity.supplierId.eq(supplierId)
                : null;
    }
}
