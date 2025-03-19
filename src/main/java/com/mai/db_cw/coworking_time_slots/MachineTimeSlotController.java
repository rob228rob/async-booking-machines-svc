package com.mai.db_cw.coworking_time_slots;

import com.mai.db_cw.coworking_time_slots.dto.MachineTimeSlotResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/machine-slots")
@RequiredArgsConstructor
public class MachineTimeSlotController {

    private final CoworkingTimeSlot coworkingTimeSlot;

    /**
     * ручка для получения всех тайм слотов по всем машинкам
     *
     * @return дто в виде списка {машинка, тйам слоты}
     */
    @GetMapping("/get-all")
    public ResponseEntity<MachineTimeSlotResponse> getAllMachineTimeSlots() {
        return ResponseEntity.ok(
                coworkingTimeSlot.getAllMachinesWithTimeSlots());
    }

    @GetMapping("/get")
    public ResponseEntity<MachineTimeSlotResponse> getMachineSlots(
            @RequestParam UUID machineId,
            @RequestParam String startDate,
            @RequestParam int weeks) {
        // Парсим startDate в LocalDate
        LocalDate start = LocalDate.parse(startDate);

        MachineTimeSlotResponse response = coworkingTimeSlot.getMachineSlots(machineId, start, weeks);

        return ResponseEntity.ok(response);
    }
}
