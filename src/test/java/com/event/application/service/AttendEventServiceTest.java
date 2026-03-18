package com.event.application.service;

import com.event.application.dto.attendance.AttendEventCommand;
import com.event.application.dto.attendance.AttendEventResult;
import com.event.application.dto.attendance.AttendEventTransactionResult;
import com.event.application.dto.attendance.AttendanceSummaryDto;
import com.event.application.dto.attendance.AttendanceWinDto;
import com.event.application.dto.attendance.PointGrantCommand;
import com.event.application.port.output.PointRewardFailureAlertPort;
import com.event.application.port.output.PointRewardPort;
import com.event.domain.model.RewardType;
import com.event.infrastructure.external.point.client.PointApiTimeoutException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendEventServiceTest {

    @Mock
    private AttendEventTransactionService attendEventTransactionService;

    @Mock
    private PointRewardPort pointRewardPort;

    @Mock
    private PointRewardFailureAlertPort pointRewardFailureAlertPort;

    @InjectMocks
    private AttendEventService attendEventService;

    @Test
    void attend_shouldReturnSuccessAndNotifyOps_whenPointApiFailsAfterCommit() {
        AttendEventResult attendEventResult = AttendEventResult.of(
                200L,
                Instant.parse("2026-03-18T00:00:00Z"),
                1,
                Boolean.TRUE,
                AttendanceWinDto.of(10L, "출석 포인트", RewardType.POINT, 30, null),
                AttendanceSummaryDto.of(1, 31)
        );
        PointGrantCommand pointGrantCommand = PointGrantCommand.of(
                1L,
                11L,
                999L,
                300L,
                30,
                "ATTENDANCE:1:11:999"
        );
        AttendEventCommand command = AttendEventCommand.of(1L, 11L, 999L);

        when(attendEventTransactionService.attend(command))
                .thenReturn(AttendEventTransactionResult.of(attendEventResult, pointGrantCommand));
        when(pointRewardPort.grant(pointGrantCommand))
                .thenThrow(PointApiTimeoutException.from("timeout", new RuntimeException("timeout")));

        AttendEventResult result = attendEventService.attend(command);

        assertThat(result).isEqualTo(attendEventResult);
        verify(pointRewardPort).grant(pointGrantCommand);
        verify(pointRewardFailureAlertPort).notifyFailure(eq(pointGrantCommand), any(PointApiTimeoutException.class));
    }

    @Test
    void attend_shouldSkipPointApi_whenRewardCommandIsAbsent() {
        AttendEventResult attendEventResult = AttendEventResult.of(
                200L,
                Instant.parse("2026-03-18T00:00:00Z"),
                1,
                Boolean.FALSE,
                null,
                AttendanceSummaryDto.of(1, 31)
        );
        AttendEventCommand command = AttendEventCommand.of(1L, 11L, 999L);

        when(attendEventTransactionService.attend(command))
                .thenReturn(AttendEventTransactionResult.of(attendEventResult, null));

        AttendEventResult result = attendEventService.attend(command);

        assertThat(result).isEqualTo(attendEventResult);
        verify(pointRewardPort, never()).grant(any());
        verify(pointRewardFailureAlertPort, never()).notifyFailure(any(), any());
    }
}
