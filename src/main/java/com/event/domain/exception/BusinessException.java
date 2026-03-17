package com.event.domain.exception;

public class BusinessException extends RuntimeException {

    private final ResponseCode responseCode;

    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
    }

    public static BusinessException from(ResponseCode responseCode) {
        return new BusinessException(responseCode);
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }
}
