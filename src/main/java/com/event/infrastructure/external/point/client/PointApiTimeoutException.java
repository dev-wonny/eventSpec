package com.event.infrastructure.external.point.client;

public class PointApiTimeoutException extends RuntimeException {

    public PointApiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public static PointApiTimeoutException from(String message, Throwable cause) {
        return new PointApiTimeoutException(message, cause);
    }
}
