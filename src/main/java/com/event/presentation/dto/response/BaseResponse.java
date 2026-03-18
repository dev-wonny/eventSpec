package com.event.presentation.dto.response;

import com.event.domain.exception.ResponseCode;
import com.event.domain.exception.code.CommonCode;
import java.time.Instant;
import lombok.Builder;

@Builder
public record BaseResponse<T>(
        String code,
        String message,
        Instant timestamp,
        T data
) {

    public static <T> BaseResponse<T> of(ResponseCode code, T data) {
        return BaseResponse.<T>builder()
                .code(code.getCode())
                .message(code.getMessage())
                .timestamp(Instant.now())
                .data(data)
                .build();
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return BaseResponse.<T>builder()
                .code(CommonCode.SUCCESS.getCode())
                .message(message)
                .timestamp(Instant.now())
                .data(data)
                .build();
    }

    public static BaseResponse<Void> error(ResponseCode code) {
        return BaseResponse.<Void>builder()
                .code(code.getCode())
                .message(code.getMessage())
                .timestamp(Instant.now())
                .data(null)
                .build();
    }

    public static <T> BaseResponse<T> error(CommonCode code, String message, T data) {
        return BaseResponse.<T>builder()
                .code(code.getCode())
                .message(message)
                .timestamp(Instant.now())
                .data(data)
                .build();
    }
}
