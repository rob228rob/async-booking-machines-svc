package com.mai.db_cw.config.infrastructure.exceptions;

public class UserAlreadyExistException extends RuntimeException {
    public UserAlreadyExistException(String msg) {
        super(msg);
    }
}
