package com.mai.db_cw.machine_time_slots;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MachineTimeSlot {
    private UUID machineId;
    private UUID timeSlotId;
    private Boolean isAvailable;
    private LocalDateTime creationTime;
    private LocalDateTime modifiedTime;
}
