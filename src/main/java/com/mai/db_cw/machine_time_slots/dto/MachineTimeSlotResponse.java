package com.mai.db_cw.machine_time_slots.dto;

import com.mai.db_cw.time_slot.dto.TimeSlotResponse;
import com.mai.db_cw.time_slot.dto.TimeSlotsResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MachineTimeSlotResponse {

    /**
     * слоты состоящие из списка с дто: ид машины, имя машины,
     * набор временных слотов машины с пометкой какой занят какой нет
     */
    private List<TimeSlotsForSingleMachine> slots;

    @Data
    @Builder
    public static class TimeSlotsForSingleMachine {
        private UUID machineId;
        private String machineName;
        private String dormitoryName;
        private String dormitoryAddress;
        private List<TimeSlotResponse> timeSlots;
    }
}
