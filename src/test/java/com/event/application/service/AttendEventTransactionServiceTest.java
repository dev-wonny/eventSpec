package com.event.application.service;

import com.event.application.dto.attendance.command.AttendEventCommand;
import com.event.application.dto.attendance.internal.AttendEventTransactionResult;
import com.event.application.dto.attendance.internal.AttendanceRewardInfo;
import com.event.application.dto.attendance.result.AttendEventResult;
import com.event.application.dto.attendance.result.AttendanceSummaryDto;
import com.event.application.dto.attendance.result.AttendanceWinDto;
import com.event.application.port.output.EventApplicantCommandPort;
import com.event.application.port.output.EventEntryQueryPort;
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
    private EventApplicantCommandPort eventApplicantCommandPort;

    @Mock
    private EventEntryQueryPort eventEntryQueryPort;

    @Mock
    private EventRoundPrizeQueryPort eventRoundPrizeQueryPort;

    @Mock
    private PrizeQueryPort prizeQueryPort;

    @Mock
    private AttendanceProcessor attendanceProcessor;

    private AttendEventTransactionService attendEventTransactionService;

    @BeforeEach
    void setUp() {
        attendEventTransactionService = new AttendEventTransactionService(
                eventQueryPort,
                eventRoundQueryPort,
                eventApplicantCommandPort,
                eventEntryQueryPort,
                eventRoundPrizeQueryPort,
                prizeQueryPort,
                new AttendanceDomainService(),
                attendanceProcessor
        );
    }

    @Test
    void attend_shouldCreateApplicantAndBuildPointGrantCommand() {
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
                .eventId(1L)
                .roundNo(1)
                .isConfirmed(false)
                .build();
        EventApplicantEntity applicant = EventApplicantEntity.builder()
                .id(100L)
                .eventId(1L)
                .roundId(11L)
                .memberId(999L)
                .build();
        EventRoundPrizeEntity eventRoundPrize = EventRoundPrizeEntity.builder()
                .id(300L)
                .roundId(11L)
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

        when(eventQueryPort.findById(1L)).thenReturn(Optional.of(event));
        when(eventRoundQueryPort.findByIdAndEventId(11L, 1L)).thenReturn(Optional.of(round));
        when(eventApplicantCommandPort.save(any(EventApplicantEntity.class))).thenReturn(applicant);
        when(eventRoundPrizeQueryPort.findActiveByRoundId(11L)).thenReturn(List.of(eventRoundPrize));
        when(prizeQueryPort.findById(400L)).thenReturn(Optional.of(prize));
        when(eventEntryQueryPort.countByEventIdAndMemberId(1L, 999L)).thenReturn(0L);
        when(eventRoundQueryPort.countByEventId(1L)).thenReturn(31L);
        when(attendanceProcessor.process(
                applicant,
                round,
                999L,
                new AttendanceRewardInfo(300L, 400L, "출석 포인트", RewardType.POINT, 30),
                1,
                31
        )).thenReturn(attendEventResult);

        AttendEventTransactionResult result = attendEventTransactionService.attend(command);

        assertThat(result.attendEventResult()).isEqualTo(attendEventResult);
        assertThat(result.pointGrantCommand()).isNotNull();
        assertThat(result.pointGrantCommand().idempotencyKey()).isEqualTo("ATTENDANCE:1:11:999");
        assertThat(result.pointGrantCommand().pointAmount()).isEqualTo(30);
        verify(eventApplicantCommandPort).save(any(EventApplicantEntity.class));
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
                .eventId(1L)
                .roundNo(1)
                .isConfirmed(false)
                .build();

        when(eventQueryPort.findById(1L)).thenReturn(Optional.of(event));
        when(eventRoundQueryPort.findByIdAndEventId(11L, 1L)).thenReturn(Optional.of(round));
        when(eventApplicantCommandPort.save(any(EventApplicantEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> attendEventTransactionService.attend(command))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getResponseCode())
                .isEqualTo(EntryCode.ENTRY_ALREADY_APPLIED);
    }
}
