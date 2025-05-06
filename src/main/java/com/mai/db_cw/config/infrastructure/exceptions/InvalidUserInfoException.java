package com.mai.db_cw.config.infrastructure.exceptions;

public class InvalidUserInfoException extends RuntimeException {
    public InvalidUserInfoException(String message) {
        super(message);
    }
}
