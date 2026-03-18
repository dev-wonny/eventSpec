package com.event.domain.exception.code;

import com.event.domain.exception.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EventCode implements ResponseCode {
    // 이벤트 급정지
    // 이벤트 종료

    EVENT_NOT_FOUND("EVENT_NOT_FOUND", "이벤트가 존재하지 않습니다.", CommonCode.NOT_FOUND),
    EVENT_NOT_ATTENDANCE("EVENT_NOT_ATTENDANCE", "출석 이벤트가 아닙니다.", CommonCode.BUSINESS_ERROR),
    EVENT_NOT_ACTIVE("EVENT_NOT_ACTIVE", "이벤트가 활성 상태가 아닙니다.", CommonCode.BUSINESS_ERROR),
    EVENT_NOT_STARTED("EVENT_NOT_STARTED", "이벤트 시작 전입니다.", CommonCode.BUSINESS_ERROR),
    EVENT_EXPIRED("EVENT_EXPIRED", "이벤트 기간이 종료되었습니다.", CommonCode.BUSINESS_ERROR),
    EVENT_ROUND_NOT_FOUND("EVENT_ROUND_NOT_FOUND", "회차가 존재하지 않습니다.", CommonCode.NOT_FOUND),
    ROUND_EVENT_MISMATCH("ROUND_EVENT_MISMATCH", "이벤트와 회차가 일치하지 않습니다.", CommonCode.BUSINESS_ERROR);

    private final String code;
    private final String message;
    private final CommonCode commonCode;

    @Override
    public HttpStatus getStatus() {
        return commonCode.getStatus();
    }
}

