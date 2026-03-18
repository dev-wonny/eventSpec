package com.event.application.service;

import com.event.application.dto.attendance.command.AttendEventCommand;
import com.event.application.dto.attendance.external.PointGrantCommand;
import com.event.application.dto.attendance.internal.AttendEventTransactionResult;
import com.event.application.dto.attendance.result.AttendEventResult;
import com.event.application.port.input.AttendEventUseCase;
import com.event.application.port.output.PointRewardFailureAlertPort;
import com.event.application.port.output.PointRewardPort;
import com.event.domain.exception.code.RewardCode;
import com.event.infrastructure.external.point.client.PointApiFailedException;
import com.event.infrastructure.external.point.client.PointApiTimeoutException;
import com.event.infrastructure.logging.LogContextKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 출석 응모 유스케이스의 진입 서비스.
 *
 * 로컬 트랜잭션 안에서 applicant/entry/win 저장과 결과 조립을 먼저 끝낸 뒤,
 * 트랜잭션 밖에서 point API를 호출한다.
 * 외부 포인트 지급이 실패해도 로컬 커밋이 끝났다면 응답은 성공으로 유지하고 운영 알림만 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendEventService implements AttendEventUseCase {

    private final AttendEventTransactionService attendEventTransactionService;
    private final PointRewardPort pointRewardPort;
    private final PointRewardFailureAlertPort pointRewardFailureAlertPort;

    @Override
    public AttendEventResult attend(AttendEventCommand command) {
        // 1. 트랜잭션 서비스에서 로컬 저장과 응답 조립에 필요한 결과를 먼저 만든다.
        AttendEventTransactionResult transactionResult = attendEventTransactionService.attend(command);
        PointGrantCommand pointGrantCommand = transactionResult.pointGrantCommand();

        // 보상 없는 출석이면 외부 포인트 API를 호출할 필요가 없다.
        if (pointGrantCommand != null) {
            try {
                // 2. 로컬 커밋 이후 외부 포인트 지급을 시도한다.
                pointRewardPort.grant(pointGrantCommand);
            } catch (PointApiTimeoutException ex) {
                // 타임아웃은 재시도/운영 확인 대상이어서 알림 포트로 넘긴다.
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
                // 응답 실패도 로컬 데이터는 이미 커밋된 상태이므로 알림만 남긴다.
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
                // 예상하지 못한 예외도 동일하게 운영 알림으로 넘겨 후속 대응이 가능하게 한다.
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

        // 3. 외부 연동 성공/실패와 관계없이 로컬 유스케이스 결과를 그대로 반환한다.
        return transactionResult.attendEventResult();
    }
}
