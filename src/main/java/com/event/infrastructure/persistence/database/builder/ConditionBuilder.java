package com.event.infrastructure.persistence.database.builder;

import com.event.application.dto.condition.SearchCondition;
import com.querydsl.core.types.Predicate;

public interface ConditionBuilder<T extends SearchCondition> {

    Predicate buildWhere(T condition);
}

