package com.cluj1.eventapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidEventOperationException extends RuntimeException {
    public InvalidEventOperationException(String message) {
        super(message);
    }
}