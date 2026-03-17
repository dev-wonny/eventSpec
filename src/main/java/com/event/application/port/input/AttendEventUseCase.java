package com.event.application.port.input;

import com.event.application.dto.attendance.AttendEventCommand;
import com.event.application.dto.attendance.AttendEventResult;

public interface AttendEventUseCase {

    AttendEventResult attend(AttendEventCommand command);
}

