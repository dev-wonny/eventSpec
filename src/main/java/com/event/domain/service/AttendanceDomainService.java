package com.event.domain.service;

import com.event.domain.entity.EventEntity;
import com.event.domain.entity.EventRoundEntity;
import com.event.domain.entity.EventRoundPrizeEntity;
import com.event.domain.entity.PrizeEntity;
import com.event.domain.exception.BusinessException;
import com.event.domain.exception.code.EventCode;
import com.event.domain.exception.code.PrizeCode;
import com.event.domain.model.EventType;
import com.event.domain.model.RewardType;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AttendanceDomainService {

    public void validateAttendable(
            EventEntity event,
            EventRoundEntity round,
            Instant now
    ) {
        if (event.getEventType() != EventType.ATTENDANCE) {
            throw BusinessException.from(EventCode.EVENT_NOT_ATTENDANCE);
        }

        if (!Boolean.TRUE.equals(event.getIsActive())) {
            throw BusinessException.from(EventCode.EVENT_NOT_ACTIVE);
        }

        if (event.getStartAt().isAfter(now)) {
            throw BusinessException.from(EventCode.EVENT_NOT_STARTED);
        }

        if (event.getEndAt().isBefore(now)) {
            throw BusinessException.from(EventCode.EVENT_EXPIRED);
        }

        if (!round.getEventId().equals(event.getId())) {
            throw BusinessException.from(EventCode.ROUND_EVENT_MISMATCH);
        }
    }

    public void validateAttendanceReward(
            List<EventRoundPrizeEntity> eventRoundPrizes,
            PrizeEntity prize
    ) {
        if (eventRoundPrizes.size() > 1) {
            throw BusinessException.from(PrizeCode.ATTENDANCE_PRIZE_CONFIGURATION_INVALID);
        }

        if (eventRoundPrizes.isEmpty()) {
            return;
        }

        if (prize == null) {
            throw BusinessException.from(PrizeCode.PRIZE_NOT_FOUND);
        }

        if (prize.getRewardType() != RewardType.POINT) {
            throw BusinessException.from(PrizeCode.PRIZE_INVALID_TYPE);
        }
    }
}
