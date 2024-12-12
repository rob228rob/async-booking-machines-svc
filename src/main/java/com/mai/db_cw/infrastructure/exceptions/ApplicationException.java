package com.mai.db_cw.infrastructure.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.Serial;

@Slf4j
public class ApplicationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ApplicationException(String message, HttpStatus status) {
        super(message);
        log.error("APP internal error: [{}] with HTTP-STATUS: [{}]", message, status);
    }
}
