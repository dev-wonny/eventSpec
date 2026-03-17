package com.event.application.port.output;

import com.event.domain.entity.PrizeEntity;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface PrizeQueryPort {

    Optional<PrizeEntity> findById(Long prizeId);

    Map<Long, PrizeEntity> findByIds(Collection<Long> prizeIds);
}

