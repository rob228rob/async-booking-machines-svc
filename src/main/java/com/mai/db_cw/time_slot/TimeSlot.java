package com.mai.db_cw.time_slot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {
    private UUID id;
    // 1=Пн, 7=Вс
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime creationTime;
    private LocalDateTime modifiedTime;
}