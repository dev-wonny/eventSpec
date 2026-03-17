package com.event.domain.exception;

import com.event.domain.exception.code.CommonCode;
import org.springframework.http.HttpStatus;

public interface ResponseCode {

    HttpStatus getStatus();

    String getCode();

    String getMessage();

    CommonCode getCommonCode();
}

