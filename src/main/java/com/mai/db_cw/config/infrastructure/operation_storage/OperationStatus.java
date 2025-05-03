package com.mai.db_cw.config.infrastructure.operation_storage;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

/**
 * статусы асинхронных операций
 */
@Getter
public enum OperationStatus implements Serializable {
    ACCEPTED,
    FINISHED_SUCCESSFULLY,
    FINISHED_UNSUCCESSFULLY,
    UNDEFINED;

    /**
     * для случая неуспешного завершения
     */
    private String cause;
    private HttpStatus httpStatus;

    public synchronized void setFailureDetails(String cause, HttpStatus httpStatus) {
        if (this != FINISHED_UNSUCCESSFULLY) {
            throw new UnsupportedOperationException("Данный метод доступен только для статуса FINISHED_UNSUCCESSFULLY");
        }
        this.cause = cause;
        this.httpStatus = httpStatus;
    }

    @Override
    public String toString() {
        return switch (this) {
            case FINISHED_SUCCESSFULLY -> "successful";
            case FINISHED_UNSUCCESSFULLY -> "unsuccessful";
            case ACCEPTED -> "accepted";
            case UNDEFINED -> "undefined";
        };
    }
}