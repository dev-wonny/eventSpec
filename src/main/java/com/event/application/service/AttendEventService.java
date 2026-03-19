package com.event.application.service;

import com.event.application.dto.attendance.command.AttendEventCommand;
import com.event.application.dto.attendance.result.AttendEventResult;
import com.event.application.port.input.AttendEventUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 출석 응모 유스케이스의 진입 서비스.
 *
 * 로컬 저장과 point after-commit event 발행은 트랜잭션 서비스에 위임하고,
 * 이 서비스는 유스케이스 진입점 역할만 맡는다.
 */
@Service
@RequiredArgsConstructor
public class AttendEventService implements AttendEventUseCase {

    private final AttendEventTransactionService attendEventTransactionService;

    @Override
    public AttendEventResult attend(AttendEventCommand command) {
        return attendEventTransactionService.attend(command);
    }
}
