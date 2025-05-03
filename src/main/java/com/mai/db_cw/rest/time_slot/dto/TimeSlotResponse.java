package com.mai.db_cw.rest.time_slot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimeSlotResponse {
    private UUID id;
    private LocalDate date;
    private int dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isAvailable;
}