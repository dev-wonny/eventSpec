package com.event.infrastructure.persistence.database.builder;

import static com.event.domain.entity.QEventEntity.eventEntity;
import static org.assertj.core.api.Assertions.assertThat;

import com.event.application.dto.condition.EventSearchCondition;
import com.event.domain.model.EventType;
import com.querydsl.core.BooleanBuilder;
import org.junit.jupiter.api.Test;

class EventEntityBuilderTest {

    private final EventEntityBuilder eventEntityBuilder = new EventEntityBuilder();

    @Test
    void buildWhere_shouldConvertEventSearchConditionToBooleanBuilder() {
        EventSearchCondition condition = EventSearchCondition.of(
                "출석",
                EventType.ATTENDANCE,
                true,
                false,
                100L
        );

        BooleanBuilder where = eventEntityBuilder.buildWhere(condition);
        BooleanBuilder expected = new BooleanBuilder()
                .and(eventEntity.isDeleted.isFalse())
                .and(eventEntity.eventName.containsIgnoreCase("출석"))
                .and(eventEntity.eventType.eq(EventType.ATTENDANCE))
                .and(eventEntity.isActive.eq(true))
                .and(eventEntity.isVisible.eq(false))
                .and(eventEntity.supplierId.eq(100L));

        assertThat(where.toString()).isEqualTo(expected.toString());
    }

    @Test
    void buildWhere_shouldKeepOnlyBaseConditionWhenSearchConditionIsEmpty() {
        BooleanBuilder where = eventEntityBuilder.buildWhere(EventSearchCondition.empty());

        assertThat(where.toString()).isEqualTo(eventEntity.isDeleted.isFalse().toString());
    }

    @Test
    void buildWhere_shouldIgnoreBlankStringCondition() {
        EventSearchCondition condition = EventSearchCondition.of(
                " ",
                null,
                null,
                null,
                null
        );

        BooleanBuilder where = eventEntityBuilder.buildWhere(condition);

        assertThat(where.toString()).isEqualTo(eventEntity.isDeleted.isFalse().toString());
    }

    @Test
    void buildWhere_shouldKeepOnlyBaseConditionWhenConditionIsNull() {
        BooleanBuilder where = eventEntityBuilder.buildWhere(null);

        assertThat(where.toString()).isEqualTo(eventEntity.isDeleted.isFalse().toString());
    }
}
