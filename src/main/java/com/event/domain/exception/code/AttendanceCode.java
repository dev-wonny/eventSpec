package com.event.domain.exception.code;

import com.event.domain.exception.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AttendanceCode implements ResponseCode {

    ATTENDANCE_NOT_AVAILABLE("ATTENDANCE_NOT_AVAILABLE", "현재 출석할 수 없습니다.", CommonCode.BUSINESS_ERROR);

    private final String code;
    private final String message;
    private final CommonCode commonCode;

    @Override
    public HttpStatus getStatus() {
        return commonCode.getStatus();
    }
}

