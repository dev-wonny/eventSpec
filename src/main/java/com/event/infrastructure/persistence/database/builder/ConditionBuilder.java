package com.event.infrastructure.persistence.database.builder;

import com.event.application.dto.condition.SearchCondition;
import com.querydsl.core.BooleanBuilder;

public interface ConditionBuilder<T extends SearchCondition> {

    BooleanBuilder buildWhere(T condition);
}
