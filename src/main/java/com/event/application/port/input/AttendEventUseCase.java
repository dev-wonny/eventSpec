package com.event.application.port.input;

import com.event.application.dto.attendance.command.AttendEventCommand;
import com.event.application.dto.attendance.result.AttendEventResult;

/**
 * 출석 응모 유스케이스 계약.
 *
 * Controller는 HTTP 입력을 Command DTO로 묶어 전달하고,
 * Service는 유스케이스 실행 결과를 Result DTO로 반환한다.
 */
public interface AttendEventUseCase {

    AttendEventResult attend(AttendEventCommand command);
}
