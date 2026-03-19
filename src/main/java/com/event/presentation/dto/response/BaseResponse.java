package com.event.presentation.dto.response;

import com.event.domain.exception.ResponseCode;
import com.event.domain.exception.code.CommonCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;

@Schema(description = "공통 API 응답")
@Builder
public record BaseResponse<T>(
        @Schema(
                description = "응답 코드. 항상 포함되며 null이 될 수 없다",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String code,
        @Schema(
                description = "응답 메시지. 항상 포함되며 null이 될 수 없다",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String message,
        @Schema(
                description = "응답 시각. 항상 포함되며 null이 될 수 없다",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant timestamp,
        @Schema(
                description = "응답 데이터. 필드는 항상 포함되며, 컬렉션은 null 대신 빈 배열을 사용한다",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
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

    public static <T> BaseResponse<T> success(T data) {
        return success(CommonCode.SUCCESS, data);
    }

    public static <T> BaseResponse<T> success(ResponseCode code, T data) {
        return BaseResponse.<T>builder()
                .code(code.getCode())
                .message(code.getMessage())
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
