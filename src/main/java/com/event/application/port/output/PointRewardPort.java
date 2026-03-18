package com.event.application.port.output;

import com.event.application.dto.attendance.external.PointGrantCommand;
import com.event.application.dto.attendance.external.PointGrantResult;

public interface PointRewardPort {

    PointGrantResult grant(PointGrantCommand command);
}
