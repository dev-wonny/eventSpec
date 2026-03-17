package com.event.domain.exception.code;

import com.event.domain.exception.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PrizeCode implements ResponseCode {

    ATTENDANCE_PRIZE_CONFIGURATION_INVALID(
            "ATTENDANCE_PRIZE_CONFIGURATION_INVALID",
            "출석 보상 설정이 올바르지 않습니다.",
            CommonCode.BUSINESS_ERROR
    ),
    PRIZE_NOT_FOUND("PRIZE_NOT_FOUND", "경품을 찾을 수 없습니다.", CommonCode.NOT_FOUND),
    PRIZE_INVALID_TYPE("PRIZE_INVALID_TYPE", "지원하지 않는 보상 유형입니다.", CommonCode.BUSINESS_ERROR);

    private final String code;
    private final String message;
    private final CommonCode commonCode;

    @Override
    public HttpStatus getStatus() {
        return commonCode.getStatus();
    }
}

