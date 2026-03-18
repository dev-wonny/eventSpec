package com.event.domain.exception.code;

import com.event.domain.exception.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EventCode implements ResponseCode {
    EVENT_NOT_FOUND("EVENT_NOT_FOUND", "이벤트가 존재하지 않습니다.", CommonCode.NOT_FOUND),
    EVENT_NOT_ATTENDANCE("EVENT_NOT_ATTENDANCE", "출석 이벤트가 아닙니다.", CommonCode.BUSINESS_ERROR),
    EVENT_NOT_ACTIVE("EVENT_NOT_ACTIVE", "현재 참여가 잠시 중단되었어요.", CommonCode.BUSINESS_ERROR),
    EVENT_NOT_STARTED("EVENT_NOT_STARTED", "이벤트 오픈 전이에요. 조금만 기다려 주세요.", CommonCode.BUSINESS_ERROR),
    EVENT_EXPIRED("EVENT_EXPIRED", "이 이벤트는 참여가 마감되었어요.", CommonCode.BUSINESS_ERROR),
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
