package com.event.application.service;

import com.event.application.dto.attendance.AttendEventResult;
import com.event.application.dto.attendance.AttendanceRewardInfo;
import com.event.application.dto.attendance.PointGrantCommand;
import com.event.application.dto.attendance.PointGrantResult;
import com.event.application.port.output.EventEntryCommandPort;
import com.event.application.port.output.EventWinCommandPort;
import com.event.application.port.output.PointRewardPort;
import com.event.domain.entity.EventApplicantEntity;
import com.event.domain.entity.EventEntryEntity;
import com.event.domain.entity.EventRoundEntity;
import com.event.domain.entity.EventWinEntity;
import com.event.domain.model.RewardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
    private EventEntryCommandPort eventEntryCommandPort;

    @Mock
    private EventWinCommandPort eventWinCommandPort;

    @Mock
    private PointRewardPort pointRewardPort;

    @InjectMocks
    private PointAttendanceProcessor pointAttendanceProcessor;

    @Captor
    private ArgumentCaptor<PointGrantCommand> pointGrantCommandCaptor;

    private EventApplicantEntity applicant;
    private EventRoundEntity round;

    @BeforeEach
    void setUp() {
        applicant = EventApplicantEntity.builder()
                .id(100L)
                .eventId(1L)
                .roundId(11L)
                .memberId(999L)
                .build();

        round = EventRoundEntity.builder()
                .id(11L)
                .eventId(1L)
                .roundNo(2)
                .isConfirmed(false)
                .build();
    }

    @Test
    void process_shouldSaveEntryOnly_whenRewardIsAbsent() {
        EventEntryEntity savedEntry = EventEntryEntity.create(
                applicant.getId(),
                applicant.getEventId(),
                round.getId(),
                applicant.getMemberId(),
                null,
                false,
                String.valueOf(applicant.getMemberId())
        );

        when(eventEntryCommandPort.save(any(EventEntryEntity.class))).thenReturn(savedEntry);

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
        verify(pointRewardPort, never()).grant(any(PointGrantCommand.class));
        verify(eventWinCommandPort, never()).save(any(EventWinEntity.class));
    }

    @Test
    void process_shouldGrantPointAndSaveWin_whenRewardExists() {
        EventEntryEntity savedEntry = EventEntryEntity.create(
                applicant.getId(),
                applicant.getEventId(),
                round.getId(),
                applicant.getMemberId(),
                300L,
                true,
                String.valueOf(applicant.getMemberId())
        );
        EventWinEntity savedWin = EventWinEntity.create(
                200L,
                round.getId(),
                applicant.getEventId(),
                applicant.getMemberId(),
                300L,
                String.valueOf(applicant.getMemberId())
        );

        when(eventEntryCommandPort.save(any(EventEntryEntity.class))).thenReturn(savedEntry);
        when(pointRewardPort.grant(any(PointGrantCommand.class))).thenReturn(new PointGrantResult("external-1"));
        when(eventWinCommandPort.save(any(EventWinEntity.class))).thenReturn(savedWin);

        AttendEventResult result = pointAttendanceProcessor.process(
                applicant,
                round,
                applicant.getMemberId(),
                new AttendanceRewardInfo(300L, 400L, "출석 포인트", RewardType.POINT, 30),
                2,
                28
        );

        verify(pointRewardPort).grant(pointGrantCommandCaptor.capture());
        PointGrantCommand command = pointGrantCommandCaptor.getValue();

        assertThat(command.idempotencyKey()).isEqualTo("ATTENDANCE:1:11:999");
        assertThat(command.pointAmount()).isEqualTo(30);
        assertThat(result.isWinner()).isTrue();
        assertThat(result.win()).isNotNull();
        assertThat(result.win().rewardType()).isEqualTo(RewardType.POINT);
    }
}
