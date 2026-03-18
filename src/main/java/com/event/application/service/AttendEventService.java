package com.event.application.service;

import com.event.application.dto.attendance.AttendEventCommand;
import com.event.application.dto.attendance.AttendEventResult;
import com.event.application.dto.attendance.AttendEventTransactionResult;
import com.event.application.dto.attendance.PointGrantCommand;
import com.event.application.port.input.AttendEventUseCase;
import com.event.application.port.output.PointRewardFailureAlertPort;
import com.event.application.port.output.PointRewardPort;
import com.event.common.logging.LogContextKeys;
import com.event.domain.exception.code.RewardCode;
import com.event.infrastructure.external.point.client.PointApiFailedException;
import com.event.infrastructure.external.point.client.PointApiTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendEventService implements AttendEventUseCase {

    private final AttendEventTransactionService attendEventTransactionService;
    private final PointRewardPort pointRewardPort;
    private final PointRewardFailureAlertPort pointRewardFailureAlertPort;

    @Override
    public AttendEventResult attend(AttendEventCommand command) {
        AttendEventTransactionResult transactionResult = attendEventTransactionService.attend(command);
        PointGrantCommand pointGrantCommand = transactionResult.pointGrantCommand();

        if (pointGrantCommand != null) {
            try {
                pointRewardPort.grant(pointGrantCommand);
            } catch (PointApiTimeoutException ex) {
                log.error(
                        "commonCode={} domainCode={} operationsAlertRequired=true {}={} {}={} {}={} idempotencyKey={}",
                        RewardCode.POINT_API_TIMEOUT.getCommonCode().getCode(),
                        RewardCode.POINT_API_TIMEOUT.getCode(),
                        LogContextKeys.EVENT_ID,
                        command.eventId(),
                        LogContextKeys.ROUND_ID,
                        command.roundId(),
                        LogContextKeys.MEMBER_ID,
                        command.memberId(),
                        pointGrantCommand.idempotencyKey(),
                        ex
                );
                pointRewardFailureAlertPort.notifyFailure(pointGrantCommand, ex);
            } catch (PointApiFailedException ex) {
                log.error(
                        "commonCode={} domainCode={} operationsAlertRequired=true {}={} {}={} {}={} idempotencyKey={}",
                        RewardCode.POINT_API_FAILED.getCommonCode().getCode(),
                        RewardCode.POINT_API_FAILED.getCode(),
                        LogContextKeys.EVENT_ID,
                        command.eventId(),
                        LogContextKeys.ROUND_ID,
                        command.roundId(),
                        LogContextKeys.MEMBER_ID,
                        command.memberId(),
                        pointGrantCommand.idempotencyKey(),
                        ex
                );
                pointRewardFailureAlertPort.notifyFailure(pointGrantCommand, ex);
            } catch (RuntimeException ex) {
                log.error(
                        "operationsAlertRequired=true pointRewardGrantUnexpectedFailure {}={} {}={} {}={} idempotencyKey={}",
                        LogContextKeys.EVENT_ID,
                        command.eventId(),
                        LogContextKeys.ROUND_ID,
                        command.roundId(),
                        LogContextKeys.MEMBER_ID,
                        command.memberId(),
                        pointGrantCommand.idempotencyKey(),
                        ex
                );
                pointRewardFailureAlertPort.notifyFailure(pointGrantCommand, ex);
            }
        }

        return transactionResult.attendEventResult();
    }
}
