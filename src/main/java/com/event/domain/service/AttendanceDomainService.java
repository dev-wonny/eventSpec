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
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 출석 이벤트 전용 도메인 규칙을 판단한다.
 *
 * Application Service가 조회한 이벤트, 회차, 보상 매핑을 받아
 * 출석 가능 여부와 보상 구성 유효성만 검증한다.
 */
@Component
public class AttendanceDomainService {

    /**
     * 출석 요청이 현재 이벤트/회차 조합에서 유효한지 검증한다.
     */
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

        if (!round.getEvent().getId().equals(event.getId())) {
            throw BusinessException.from(EventCode.ROUND_EVENT_MISMATCH);
        }
    }

    public void validateAttendanceReward(
            List<EventRoundPrizeEntity> eventRoundPrizes,
            PrizeEntity prize
    ) {
        // 출석 회차는 무보상 또는 단일 포인트 보상만 허용한다.
        if (eventRoundPrizes.size() > 1) {
            throw BusinessException.from(PrizeCode.ATTENDANCE_PRIZE_CONFIGURATION_INVALID);
        }

        if (eventRoundPrizes.isEmpty()) {
            return;
        }

        if (Objects.isNull(prize)) {
            throw BusinessException.from(PrizeCode.PRIZE_NOT_FOUND);
        }

        if (prize.getRewardType() != RewardType.POINT) {
            throw BusinessException.from(PrizeCode.PRIZE_INVALID_TYPE);
        }
    }
}
