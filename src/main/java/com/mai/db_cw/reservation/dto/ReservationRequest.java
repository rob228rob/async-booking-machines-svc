package com.mai.db_cw.reservation.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Класс дто для получения запроса на бронирование машинки
 *
 * @param machineId
 * @param resDate
 * @param startTime
 * @param endTime
 */
public record ReservationRequest(
        UUID machineId,
        LocalDate resDate,
        LocalTime startTime,
        LocalTime endTime
) implements Serializable {
}
