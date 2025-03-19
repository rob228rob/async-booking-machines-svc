package com.mai.db_cw.coworkings.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * класс-дто который отдает для логов в админку
 */
@Data
@Builder
public class ReservationLog {
    private UUID reservationId;
    private String action;
    private String oldData; // JSON-строка с предыдущими данными
    private String newData; // JSON-строка с новыми данными
    private LocalDateTime timestamp;
}

