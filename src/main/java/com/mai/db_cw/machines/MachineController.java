package com.mai.db_cw.machines;

import com.fasterxml.uuid.Generators;
import com.mai.db_cw.machines.dto.MachineRequest;
import com.mai.db_cw.machines.dto.MachineResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/machines")
@Slf4j
@RequiredArgsConstructor
public class MachineController {
    private final MachineService machineService;

    /**
     * получение всех машинок
     *
     * @return
     */
    @GetMapping("/get-all")
    public ResponseEntity<List<MachineResponse>> getAllMachines() {
        return ResponseEntity
                .ok(machineService.findAllMachines());
    }

    /**
     * асинхронный метод добавления новой машинки
     * доступен ток админу
     *
     * @param machineRequest
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<UUID> addMachine(@RequestBody MachineRequest machineRequest) {
        UUID randomId = Generators.timeBasedEpochGenerator().generate();
        machineService.runAsyncCreateMachine(randomId, machineRequest);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .header("Location", "/api/machines/" + randomId.toString())
                .body(randomId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{machineId}")
    public ResponseEntity<Void> getMachine(@PathVariable UUID machineId) {
        boolean added = machineService.findById(machineId).isPresent();

        return added
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/del/{machineId}")
    public ResponseEntity<UUID> deleteMachine(@PathVariable UUID machineId) {
        machineService.deleteAsyncById(machineId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(machineId);
    }
}
