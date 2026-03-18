package com.event.infrastructure.notification;

import com.event.application.dto.attendance.PointGrantCommand;
import com.event.application.port.output.PointRewardFailureAlertPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PointRewardFailureAlertImpl implements PointRewardFailureAlertPort {

    @Override
    public void notifyFailure(PointGrantCommand command, Throwable throwable) {
        log.error(
                "operationsAlert=true pointRewardGrantFailedAfterLocalCommit eventId={} roundId={} memberId={} eventRoundPrizeId={} idempotencyKey={}",
                command.eventId(),
                command.roundId(),
                command.memberId(),
                command.eventRoundPrizeId(),
                command.idempotencyKey(),
                throwable
        );
    }
}
