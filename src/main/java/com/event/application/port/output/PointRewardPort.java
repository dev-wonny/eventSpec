package com.event.application.port.output;

import com.event.application.dto.attendance.PointGrantCommand;
import com.event.application.dto.attendance.PointGrantResult;

public interface PointRewardPort {

    PointGrantResult grant(PointGrantCommand command);
}
