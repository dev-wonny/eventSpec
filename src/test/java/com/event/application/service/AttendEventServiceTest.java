package com.event.application.service;

import com.event.application.dto.attendance.command.AttendEventCommand;
import com.event.application.dto.attendance.result.AttendEventResult;
import com.event.application.dto.attendance.result.AttendanceSummaryDto;
import com.event.application.dto.attendance.result.AttendanceWinDto;
import com.event.domain.model.RewardType;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendEventServiceTest {

    @Mock
    private AttendEventTransactionService attendEventTransactionService;

    @InjectMocks
    private AttendEventService attendEventService;

    @Test
    void attend_shouldReturnTransactionServiceResult() {
        AttendEventResult attendEventResult = AttendEventResult.of(
                200L,
                Instant.parse("2026-03-18T00:00:00Z"),
                1,
                Boolean.TRUE,
                AttendanceWinDto.of(10L, "출석 포인트", RewardType.POINT, 30, null),
                AttendanceSummaryDto.of(1, 31)
        );
        AttendEventCommand command = AttendEventCommand.of(1L, 11L, 999L);

        when(attendEventTransactionService.attend(command)).thenReturn(attendEventResult);

        AttendEventResult result = attendEventService.attend(command);

        assertThat(result).isEqualTo(attendEventResult);
    }
}
