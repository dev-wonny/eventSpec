package com.event.domain.service;

import com.event.domain.entity.EventApplicantEntity;
import com.event.domain.entity.EventEntity;
import com.event.domain.entity.EventRoundEntity;
import com.event.domain.entity.EventRoundPrizeEntity;
import com.event.domain.entity.PrizeEntity;
import com.event.domain.exception.BusinessException;
import com.event.domain.exception.code.EntryCode;
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

        EventApplicantEntity applicant = EventApplicantEntity.builder()
                .id(100L)
                .eventId(1L)
                .roundId(10L)
                .memberId(999L)
                .build();

        assertThatCode(() -> attendanceDomainService.validateAttendable(
                event,
                round,
                applicant,
                false,
                Instant.now()
        )).doesNotThrowAnyException();
    }

    @Test
    void validateAttendable_shouldThrow_whenAlreadyApplied() {
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

        EventApplicantEntity applicant = EventApplicantEntity.builder()
                .id(100L)
                .eventId(1L)
                .roundId(10L)
                .memberId(999L)
                .build();

        assertThatThrownBy(() -> attendanceDomainService.validateAttendable(
                event,
                round,
                applicant,
                true,
                Instant.now()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getResponseCode())
                .isEqualTo(EntryCode.ENTRY_ALREADY_APPLIED);
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

