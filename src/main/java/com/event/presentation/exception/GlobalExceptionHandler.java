package com.event.presentation.exception;

import com.event.domain.exception.BusinessException;
import com.event.domain.exception.code.CommonCode;
import com.event.presentation.dto.response.BaseResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException ex) {
        var code = ex.getResponseCode();

        log.warn("commonCode={} domainCode={} message={}",
                code.getCommonCode().getCode(),
                code.getCode(),
                code.getMessage(),
                ex);

        return ResponseEntity
                .status(code.getStatus())
                .body(BaseResponse.error(code));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        return ResponseEntity
                .status(CommonCode.INVALID_REQUEST.getStatus())
                .body(BaseResponse.error(
                        CommonCode.INVALID_REQUEST,
                        CommonCode.INVALID_REQUEST.getMessage(),
                        errors
                ));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleMissingHeaderException(
            MissingRequestHeaderException ex
    ) {
        Map<String, String> errors = Map.of(
                ex.getHeaderName(),
                ex.getHeaderName() + " 헤더는 필수입니다."
        );

        return ResponseEntity
                .status(CommonCode.INVALID_REQUEST.getStatus())
                .body(BaseResponse.error(
                        CommonCode.INVALID_REQUEST,
                        CommonCode.INVALID_REQUEST.getMessage(),
                        errors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex
    ) {
        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        return ResponseEntity
                .status(CommonCode.INVALID_REQUEST.getStatus())
                .body(BaseResponse.error(
                        CommonCode.INVALID_REQUEST,
                        CommonCode.INVALID_REQUEST.getMessage(),
                        errors
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex
    ) {
        return ResponseEntity
                .status(CommonCode.INVALID_REQUEST.getStatus())
                .body(BaseResponse.<Void>error(
                        CommonCode.INVALID_REQUEST,
                        CommonCode.INVALID_REQUEST.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex
    ) {
        String parameterName = ex.getName() != null ? ex.getName() : "request";
        Map<String, String> errors = Map.of(
                parameterName,
                parameterName + " 값의 형식이 올바르지 않습니다."
        );

        return ResponseEntity
                .status(CommonCode.INVALID_REQUEST.getStatus())
                .body(BaseResponse.error(
                        CommonCode.INVALID_REQUEST,
                        CommonCode.INVALID_REQUEST.getMessage(),
                        errors
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception ex) {
        log.error("commonCode={} message={}", CommonCode.INTERNAL_ERROR.getCode(), ex.getMessage(), ex);
        return ResponseEntity
                .status(CommonCode.INTERNAL_ERROR.getStatus())
                .body(BaseResponse.<Void>error(
                        CommonCode.INTERNAL_ERROR,
                        CommonCode.INTERNAL_ERROR.getMessage(),
                        null
                ));
    }
}
