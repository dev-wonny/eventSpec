package com.event.domain.exception.code;

import com.event.domain.exception.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RewardCode implements ResponseCode {

    POINT_API_TIMEOUT(
            "POINT_API_TIMEOUT",
            "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
            CommonCode.INTERNAL_ERROR
    ),
    POINT_API_FAILED(
            "POINT_API_FAILED",
            "출석 처리에 실패했습니다. 잠시 후 다시 시도해주세요.",
            CommonCode.INTERNAL_ERROR
    );

    private final String code;
    private final String message;
    private final CommonCode commonCode;

    @Override
    public HttpStatus getStatus() {
        return commonCode.getStatus();
    }
}

