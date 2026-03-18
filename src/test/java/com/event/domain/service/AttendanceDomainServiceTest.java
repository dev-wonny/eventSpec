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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttendanceDomainServiceTest {

    private AttendanceDomainService attendanceDomainService;

    @BeforeEach
    void setUp() {
        attendanceDomainService = new AttendanceDomainService();
    }

    @Test
    void validateAttendable_shouldPass_whenAttendanceIsAvailable() {
        EventEntity event = EventEntity.builder()
                .id(1L)
                .eventName("attendance")
                .eventType(EventType.ATTENDANCE)
                .startAt(Instant.now().minusSeconds(3600))
                .endAt(Instant.now().plusSeconds(3600))
                .supplierId(1L)
                .priority(0)
                .isActive(true)
                .isVisible(true)
                .isAutoEntry(false)
                .isSnsLinked(false)
                .isWinnerAnnounced(false)
                .isDuplicateWinner(false)
                .isMultipleEntry(false)
                .build();

        EventRoundEntity round = EventRoundEntity.builder()
                .id(10L)
                .eventId(1L)
                .roundNo(1)
                .isConfirmed(false)
                .build();

        assertThatCode(() -> attendanceDomainService.validateAttendable(
                event,
                round,
                Instant.now()
        )).doesNotThrowAnyException();
    }

    @Test
    void validateAttendable_shouldThrow_whenRoundDoesNotBelongToEvent() {
        EventEntity event = EventEntity.builder()
                .id(1L)
                .eventName("attendance")
                .eventType(EventType.ATTENDANCE)
                .startAt(Instant.now().minusSeconds(3600))
                .endAt(Instant.now().plusSeconds(3600))
                .supplierId(1L)
                .priority(0)
                .isActive(true)
                .isVisible(true)
                .isAutoEntry(false)
                .isSnsLinked(false)
                .isWinnerAnnounced(false)
                .isDuplicateWinner(false)
                .isMultipleEntry(false)
                .build();

        EventRoundEntity round = EventRoundEntity.builder()
                .id(10L)
                .eventId(2L)
                .roundNo(1)
                .isConfirmed(false)
                .build();

        assertThatThrownBy(() -> attendanceDomainService.validateAttendable(
                event,
                round,
                Instant.now()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getResponseCode())
                .isEqualTo(EventCode.ROUND_EVENT_MISMATCH);
    }

    @Test
    void validateAttendable_shouldThrowFriendlyMessage_whenEventIsInactive() {
        EventEntity event = EventEntity.builder()
                .id(1L)
                .eventName("attendance")
                .eventType(EventType.ATTENDANCE)
                .startAt(Instant.now().minusSeconds(3600))
                .endAt(Instant.now().plusSeconds(3600))
                .supplierId(1L)
                .priority(0)
                .isActive(false)
                .isVisible(true)
                .isAutoEntry(false)
                .isSnsLinked(false)
                .isWinnerAnnounced(false)
                .isDuplicateWinner(false)
                .isMultipleEntry(false)
                .build();

        EventRoundEntity round = EventRoundEntity.builder()
                .id(10L)
                .eventId(1L)
                .roundNo(1)
                .isConfirmed(false)
                .build();

        assertThatThrownBy(() -> attendanceDomainService.validateAttendable(
                event,
                round,
                Instant.now()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getResponseCode())
                .isEqualTo(EventCode.EVENT_NOT_ACTIVE);

        assertThatThrownBy(() -> attendanceDomainService.validateAttendable(
                event,
                round,
                Instant.now()
        ))
                .hasMessage("현재 참여가 잠시 중단되었어요.");
    }

    @Test
    void validateAttendable_shouldThrowFriendlyMessage_whenEventIsNotStarted() {
        EventEntity event = EventEntity.builder()
                .id(1L)
                .eventName("attendance")
                .eventType(EventType.ATTENDANCE)
                .startAt(Instant.now().plusSeconds(3600))
                .endAt(Instant.now().plusSeconds(7200))
                .supplierId(1L)
                .priority(0)
                .isActive(true)
                .isVisible(true)
                .isAutoEntry(false)
                .isSnsLinked(false)
                .isWinnerAnnounced(false)
                .isDuplicateWinner(false)
                .isMultipleEntry(false)
                .build();

        EventRoundEntity round = EventRoundEntity.builder()
                .id(10L)
                .eventId(1L)
                .roundNo(1)
                .isConfirmed(false)
                .build();

        assertThatThrownBy(() -> attendanceDomainService.validateAttendable(
                event,
                round,
                Instant.now()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getResponseCode())
                .isEqualTo(EventCode.EVENT_NOT_STARTED);

        assertThatThrownBy(() -> attendanceDomainService.validateAttendable(
                event,
                round,
                Instant.now()
        ))
                .hasMessage("이벤트 오픈 전이에요. 조금만 기다려 주세요.");
    }

    @Test
    void validateAttendable_shouldThrowFriendlyMessage_whenEventIsExpired() {
        EventEntity event = EventEntity.builder()
                .id(1L)
                .eventName("attendance")
                .eventType(EventType.ATTENDANCE)
                .startAt(Instant.now().minusSeconds(7200))
                .endAt(Instant.now().minusSeconds(3600))
                .supplierId(1L)
                .priority(0)
                .isActive(true)
                .isVisible(true)
                .isAutoEntry(false)
                .isSnsLinked(false)
                .isWinnerAnnounced(false)
                .isDuplicateWinner(false)
                .isMultipleEntry(false)
                .build();

        EventRoundEntity round = EventRoundEntity.builder()
                .id(10L)
                .eventId(1L)
                .roundNo(1)
                .isConfirmed(false)
                .build();

        assertThatThrownBy(() -> attendanceDomainService.validateAttendable(
                event,
                round,
                Instant.now()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getResponseCode())
                .isEqualTo(EventCode.EVENT_EXPIRED);

        assertThatThrownBy(() -> attendanceDomainService.validateAttendable(
                event,
                round,
                Instant.now()
        ))
                .hasMessage("이 이벤트는 참여가 마감되었어요.");
    }

    @Test
    void validateAttendanceReward_shouldThrow_whenActiveRewardsAreMoreThanOne() {
        EventRoundPrizeEntity first = EventRoundPrizeEntity.builder()
                .id(1L)
                .roundId(10L)
                .prizeId(100L)
                .priority(0)
                .isActive(true)
                .build();
        EventRoundPrizeEntity second = EventRoundPrizeEntity.builder()
                .id(2L)
                .roundId(10L)
                .prizeId(101L)
                .priority(1)
                .isActive(true)
                .build();

        PrizeEntity prize = PrizeEntity.builder()
                .id(100L)
                .prizeName("point")
                .rewardType(RewardType.POINT)
                .pointAmount(30)
                .isActive(true)
                .build();

        assertThatThrownBy(() -> attendanceDomainService.validateAttendanceReward(
                List.of(first, second),
                prize
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getResponseCode())
                .isEqualTo(PrizeCode.ATTENDANCE_PRIZE_CONFIGURATION_INVALID);
    }

    @Test
    void validateAttendanceReward_shouldThrow_whenRewardTypeIsNotPoint() {
        EventRoundPrizeEntity reward = EventRoundPrizeEntity.builder()
                .id(1L)
                .roundId(10L)
                .prizeId(100L)
                .priority(0)
                .isActive(true)
                .build();

        PrizeEntity prize = PrizeEntity.builder()
                .id(100L)
                .prizeName("coupon")
                .rewardType(RewardType.COUPON)
                .isActive(true)
                .build();

        assertThatThrownBy(() -> attendanceDomainService.validateAttendanceReward(
                List.of(reward),
                prize
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getResponseCode())
                .isEqualTo(PrizeCode.PRIZE_INVALID_TYPE);
    }
}
