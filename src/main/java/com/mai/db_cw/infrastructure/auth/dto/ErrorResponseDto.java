package com.mai.db_cw.infrastructure.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto {

    private String message;

    private LocalDateTime timestamp;

    private int status;

    public ErrorResponseDto(String message, int value) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.status = value;
    }
}
