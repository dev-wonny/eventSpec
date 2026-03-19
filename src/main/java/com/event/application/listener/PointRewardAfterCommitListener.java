package com.event.application.listener;

import com.event.application.dto.attendance.external.PointGrantCommand;
import com.event.application.port.output.PointRewardFailureAlertPort;
import com.event.application.port.output.PointRewardPort;
import com.event.domain.exception.code.CommonCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 로컬 트랜잭션 commit 이후에만 point API를 호출하는 after-commit listener.
 *
 * 외부 지급 실패는 사용자 응답 오류로 바꾸지 않고 운영 알림 대상으로만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointRewardAfterCommitListener {

    private static final String EVENT_ID_LOG_KEY = "eventId";
    private static final String ROUND_ID_LOG_KEY = "roundId";
    private static final String MEMBER_ID_LOG_KEY = "memberId";

    private final PointRewardPort pointRewardPort;
    private final PointRewardFailureAlertPort pointRewardFailureAlertPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PointGrantCommand command) {
        try {
            pointRewardPort.grant(command);
        } catch (RuntimeException ex) {
            logFailure(command, ex);
        }
    }

    private void logFailure(PointGrantCommand command, Throwable throwable) {
        log.error(
                "commonCode={} operationsAlertRequired=true pointRewardGrantFailed {}={} {}={} {}={} idempotencyKey={}",
                CommonCode.INTERNAL_ERROR.getCode(),
                EVENT_ID_LOG_KEY,
                command.eventId(),
                ROUND_ID_LOG_KEY,
                command.roundId(),
                MEMBER_ID_LOG_KEY,
                command.memberId(),
                command.idempotencyKey(),
                throwable
        );
        pointRewardFailureAlertPort.notifyFailure(command, throwable);
    }
}
