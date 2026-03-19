package com.event.application.service;

import com.event.application.dto.attendance.command.AttendEventCommand;
import com.event.application.dto.attendance.external.PointGrantCommand;
import com.event.application.dto.attendance.internal.AttendanceRewardInfo;
import com.event.application.dto.attendance.result.AttendEventResult;
import com.event.application.dto.attendance.result.AttendanceSummaryDto;
import com.event.application.dto.attendance.result.AttendanceWinDto;
import com.event.application.port.output.EventApplicantRepositoryPort;
import com.event.application.port.output.EventQueryPort;
import com.event.application.port.output.EventRoundPrizeQueryPort;
import com.event.application.port.output.EventRoundQueryPort;
import com.event.application.port.output.PrizeQueryPort;
import com.event.domain.entity.EventApplicantEntity;
import com.event.domain.entity.EventEntity;
import com.event.domain.entity.EventRoundEntity;
import com.event.domain.entity.EventRoundPrizeEntity;
import com.event.domain.entity.PrizeEntity;
import com.event.domain.exception.BusinessException;
import com.event.domain.exception.code.EntryCode;
import com.event.domain.exception.code.EventCode;
import com.event.domain.model.EventType;
import com.event.domain.model.RewardType;
import com.event.domain.service.AttendanceDomainService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendEventTransactionServiceTest {

    @Mock
    private EventQueryPort eventQueryPort;

    @Mock
    private EventRoundQueryPort eventRoundQueryPort;

    @Mock
    private EventApplicantRepositoryPort eventApplicantRepositoryPort;

    @Mock
    private EventRoundPrizeQueryPort eventRoundPrizeQueryPort;

    @Mock
    private PrizeQueryPort prizeQueryPort;

    @Mock
    private AttendanceProcessor attendanceProcessor;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private AttendEventTransactionService attendEventTransactionService;

    @BeforeEach
    void setUp() {
        attendEventTransactionService = new AttendEventTransactionService(
                eventQueryPort,
                eventRoundQueryPort,
                eventApplicantRepositoryPort,
                eventRoundPrizeQueryPort,
                prizeQueryPort,
                new AttendanceDomainService(),
                attendanceProcessor,
                applicationEventPublisher
        );
    }

    @Test
    void attend_shouldCreateApplicantAndPublishPointGrantEvent() {
        AttendEventCommand command = AttendEventCommand.of(1L, 11L, 999L);
        EventEntity event = EventEntity.builder()
                .id(1L)
                .eventName("attendance")
                .eventType(EventType.ATTENDANCE)
                .startAt(Instant.now().minusSeconds(3600))
                .endAt(Instant.now().plusSeconds(3600))
                .supplierId(1L)
                .priority(1)
                .isActive(true)
                .isVisible(true)
                .isAutoEntry(false)
                .isSnsLinked(false)
                .isWinnerAnnounced(false)
                .isDuplicateWinner(false)
                .isMultipleEntry(false)
                .build();
        EventRoundEntity round = EventRoundEntity.builder()
                .id(11L)
                .event(event)
                .roundNo(1)
                .isConfirmed(false)
                .build();
        EventApplicantEntity applicant = EventApplicantEntity.builder()
                .id(100L)
                .event(event)
                .round(round)
                .memberId(999L)
                .build();
        EventRoundPrizeEntity eventRoundPrize = EventRoundPrizeEntity.builder()
                .id(300L)
                .round(round)
                .prizeId(400L)
                .priority(0)
                .isActive(true)
                .build();
        PrizeEntity prize = PrizeEntity.builder()
                .id(400L)
                .prizeName("출석 포인트")
                .rewardType(RewardType.POINT)
                .pointAmount(30)
                .isActive(true)
                .build();
        AttendEventResult attendEventResult = AttendEventResult.of(
                200L,
                Instant.parse("2026-03-18T00:00:00Z"),
                1,
                Boolean.TRUE,
                AttendanceWinDto.of(10L, "출석 포인트", RewardType.POINT, 30, null),
                AttendanceSummaryDto.of(1, 31)
        );

        when(eventRoundQueryPort.findById(11L)).thenReturn(Optional.of(round));
        when(eventApplicantRepositoryPort.findByEventIdAndMemberIdForUpdate(1L, 999L)).thenReturn(List.of());
        when(eventApplicantRepositoryPort.save(any(EventApplicantEntity.class))).thenReturn(applicant);
        when(eventRoundPrizeQueryPort.findActiveByRoundId(11L)).thenReturn(List.of(eventRoundPrize));
        when(prizeQueryPort.findById(400L)).thenReturn(Optional.of(prize));
        when(eventRoundQueryPort.countByEventId(1L)).thenReturn(31L);
        when(attendanceProcessor.process(
                applicant,
                round,
                999L,
                new AttendanceRewardInfo(300L, 400L, "출석 포인트", RewardType.POINT, 30),
                1L,
                31L
        )).thenReturn(attendEventResult);

        AttendEventResult result = attendEventTransactionService.attend(command);

        assertThat(result).isEqualTo(attendEventResult);
        verify(eventApplicantRepositoryPort).save(any(EventApplicantEntity.class));
        verify(applicationEventPublisher).publishEvent(PointGrantCommand.of(
                1L,
                11L,
                999L,
                300L,
                30,
                "ATTENDANCE:1:11:999"
        ));
    }

    @Test
    void attend_shouldThrowAlreadyApplied_whenApplicantInsertConflicts() {
        AttendEventCommand command = AttendEventCommand.of(1L, 11L, 999L);
        EventEntity event = EventEntity.builder()
                .id(1L)
                .eventName("attendance")
                .eventType(EventType.ATTENDANCE)
                .startAt(Instant.now().minusSeconds(3600))
                .endAt(Instant.now().plusSeconds(3600))
                .supplierId(1L)
                .priority(1)
                .isActive(true)
                .isVisible(true)
                .isAutoEntry(false)
                .isSnsLinked(false)
                .isWinnerAnnounced(false)
                .isDuplicateWinner(false)
                .isMultipleEntry(false)
                .build();
        EventRoundEntity round = EventRoundEntity.builder()
                .id(11L)
                .event(event)
                .roundNo(1)
                .isConfirmed(false)
                .build();

        when(eventRoundQueryPort.findById(11L)).thenReturn(Optional.of(round));
        when(eventApplicantRepositoryPort.findByEventIdAndMemberIdForUpdate(1L, 999L)).thenReturn(List.of());
        when(eventApplicantRepositoryPort.save(any(EventApplicantEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> attendEventTransactionService.attend(command))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getResponseCode())
                .isEqualTo(EntryCode.ENTRY_ALREADY_APPLIED);
    }

    @Test
    void attend_shouldThrowRoundEventMismatch_whenRoundBelongsToAnotherEvent() {
        AttendEventCommand command = AttendEventCommand.of(1L, 11L, 999L);
        EventEntity event = EventEntity.builder()
                .id(1L)
                .eventName("attendance")
                .eventType(EventType.ATTENDANCE)
                .startAt(Instant.now().minusSeconds(3600))
                .endAt(Instant.now().plusSeconds(3600))
                .supplierId(1L)
                .priority(1)
                .isActive(true)
                .isVisible(true)
                .isAutoEntry(false)
                .isSnsLinked(false)
                .isWinnerAnnounced(false)
                .isDuplicateWinner(false)
                .isMultipleEntry(false)
                .build();
        EventEntity otherEvent = EventEntity.builder()
                .id(2L)
                .eventName("other")
                .eventType(EventType.ATTENDANCE)
                .startAt(Instant.now().minusSeconds(3600))
                .endAt(Instant.now().plusSeconds(3600))
                .supplierId(1L)
                .priority(1)
                .isActive(true)
                .isVisible(true)
                .isAutoEntry(false)
                .isSnsLinked(false)
                .isWinnerAnnounced(false)
                .isDuplicateWinner(false)
                .isMultipleEntry(false)
                .build();
        EventRoundEntity round = EventRoundEntity.builder()
                .id(11L)
                .event(otherEvent)
                .roundNo(1)
                .isConfirmed(false)
                .build();

        when(eventQueryPort.findById(1L)).thenReturn(Optional.of(event));
        when(eventRoundQueryPort.findById(11L)).thenReturn(Optional.of(round));

        assertThatThrownBy(() -> attendEventTransactionService.attend(command))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getResponseCode())
                .isEqualTo(EventCode.ROUND_EVENT_MISMATCH);
    }
}
