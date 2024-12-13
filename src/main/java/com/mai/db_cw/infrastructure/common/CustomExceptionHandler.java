package com.mai.db_cw.infrastructure.common;

import com.mai.db_cw.infrastructure.auth.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mai.db_cw.infrastructure.exceptions.InvalidUserInfoException;
import com.mai.db_cw.infrastructure.exceptions.UserNotFoundException;

import java.util.Arrays;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> notFoundException(UserNotFoundException exception) {
        log.error("Exception occure here {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto(exception.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(InvalidUserInfoException.class)
    public ResponseEntity<ErrorResponseDto> invalidUserInfoException(InvalidUserInfoException exception) {
        log.error("Exception occure here {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto(exception.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> badCredentialsException(BadCredentialsException exception) {
        log.error("Exception occure here {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponseDto(exception.getMessage(), HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto> runtimeException(RuntimeException exception) {
        log.error("Exception occure here {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto(exception.getMessage() + "\n" + Arrays.toString(exception.getStackTrace()), HttpStatus.BAD_REQUEST.value()));
    }
}
