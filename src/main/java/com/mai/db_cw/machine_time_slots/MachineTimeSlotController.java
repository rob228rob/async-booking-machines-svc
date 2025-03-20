package com.mai.db_cw.machine_time_slots;

import com.mai.db_cw.machine_time_slots.dto.MachineTimeSlotResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Контроллер для работы со слотами коворкингов (раньше «машинки»).
 */
@Slf4j
@RestController
@RequestMapping("/api/machine-slots")
@RequiredArgsConstructor
public class MachineTimeSlotController {

    private final CoworkingSlotService coworkingSlotService;

    /**
     * Получить все слоты по всем коворкингам (раньше machines).
     */
    @GetMapping("/get-all")
    public ResponseEntity<MachineTimeSlotResponse> getAllMachineTimeSlots() {
        return ResponseEntity.ok(
                coworkingSlotService.getAllCoworkingsWithTimeSlots()
        );
    }

    /**
     * Получить слоты для одного коворкинга (machineId → coworkingId),
     * начиная с даты startDate, на кол-во недель weeks.
     */
    @GetMapping("/get")
    public ResponseEntity<MachineTimeSlotResponse> getMachineSlots(
            @RequestParam UUID machineId,
            @RequestParam String startDate,
            @RequestParam int weeks
    ) {
        // Парсим строку в LocalDate
        LocalDate start = LocalDate.parse(startDate);

        // Переименуйте machineId → coworkingId на фронте, если хотите более точного соответствия
        MachineTimeSlotResponse response = coworkingSlotService.getCoworkingSlots(machineId, start, weeks);
        return ResponseEntity.ok(response);
    }
}
