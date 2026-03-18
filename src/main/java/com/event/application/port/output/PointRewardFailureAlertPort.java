package com.event.application.port.output;

import com.event.application.dto.attendance.external.PointGrantCommand;

public interface PointRewardFailureAlertPort {

    void notifyFailure(PointGrantCommand command, Throwable throwable);
}
