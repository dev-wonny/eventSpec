package com.event.application.service;

import com.event.application.dto.attendance.internal.AttendanceRewardInfo;
import com.event.application.dto.attendance.result.AttendEventResult;
import com.event.application.port.output.EventEntryRepositoryPort;
import com.event.application.port.output.EventWinCommandPort;
import com.event.domain.entity.EventApplicantEntity;
import com.event.domain.entity.EventEntity;
import com.event.domain.entity.EventEntryEntity;
import com.event.domain.entity.EventRoundEntity;
import com.event.domain.entity.EventWinEntity;
import com.event.domain.model.RewardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointAttendanceProcessorTest {

    @Mock
    private EventEntryRepositoryPort eventEntryRepositoryPort;

    @Mock
    private EventWinCommandPort eventWinCommandPort;

    @InjectMocks
    private PointAttendanceProcessor pointAttendanceProcessor;

    private EventApplicantEntity applicant;
    private EventRoundEntity round;

    @BeforeEach
    void setUp() {
        EventEntity event = EventEntity.builder()
                .id(1L)
                .eventName("attendance")
                .build();

        round = EventRoundEntity.builder()
                .id(11L)
                .event(event)
                .roundNo(2)
                .isConfirmed(false)
                .build();

        applicant = EventApplicantEntity.builder()
                .id(100L)
                .event(event)
                .round(round)
                .memberId(999L)
                .build();
    }

    @Test
    void process_shouldSaveEntryOnly_whenRewardIsAbsent() {
        EventEntryEntity savedEntry = EventEntryEntity.create(
                applicant,
                applicant.getMemberId(),
                null,
                false,
                applicant.getMemberId()
        );

        when(eventEntryRepositoryPort.save(any(EventEntryEntity.class))).thenReturn(savedEntry);

        AttendEventResult result = pointAttendanceProcessor.process(
                applicant,
                round,
                applicant.getMemberId(),
                null,
                1,
                28
        );

        assertThat(result.isWinner()).isFalse();
        assertThat(result.win()).isNull();
        verify(eventWinCommandPort, never()).save(any(EventWinEntity.class));
    }

    @Test
    void process_shouldSaveWin_whenRewardExists() {
        EventEntryEntity savedEntry = EventEntryEntity.create(
                applicant,
                applicant.getMemberId(),
                300L,
                true,
                applicant.getMemberId()
        );
        EventWinEntity savedWin = EventWinEntity.create(
                200L,
                round,
                applicant.getMemberId(),
                300L,
                applicant.getMemberId()
        );

        when(eventEntryRepositoryPort.save(any(EventEntryEntity.class))).thenReturn(savedEntry);
        when(eventWinCommandPort.save(any(EventWinEntity.class))).thenReturn(savedWin);

        AttendEventResult result = pointAttendanceProcessor.process(
                applicant,
                round,
                applicant.getMemberId(),
                new AttendanceRewardInfo(300L, 400L, "출석 포인트", RewardType.POINT, 30),
                2,
                28
        );

        verify(eventWinCommandPort).save(any(EventWinEntity.class));
        assertThat(result.isWinner()).isTrue();
        assertThat(result.win()).isNotNull();
        assertThat(result.win().rewardType()).isEqualTo(RewardType.POINT);
    }
}
