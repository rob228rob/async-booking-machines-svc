package com.mai.db_cw.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 *  Класс для учета бронирований, который
 *  учитывает айди юзера, айди машинки и таймслот
 */
@Data
@Builder
public class Reservation {
    private UUID id;
    private UUID userId;
    private UUID machineId;
    private LocalDate resDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private LocalDateTime creationTime;
    private LocalDateTime modifiedTime;
}