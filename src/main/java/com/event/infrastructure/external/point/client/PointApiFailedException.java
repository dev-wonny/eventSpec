package com.event.infrastructure.external.point.client;

public class PointApiFailedException extends RuntimeException {

    public PointApiFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public static PointApiFailedException from(String message, Throwable cause) {
        return new PointApiFailedException(message, cause);
    }
}
