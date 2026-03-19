package com.event.application.port.output;

import com.event.application.dto.attendance.external.PointGrantCommand;
import com.event.application.dto.attendance.external.PointGrantResult;

/**
 * 외부 api 호출
 * 포인트 적립을 위한 포트 인터페이스
 */
public interface PointRewardPort {

    PointGrantResult grant(PointGrantCommand command);
}
