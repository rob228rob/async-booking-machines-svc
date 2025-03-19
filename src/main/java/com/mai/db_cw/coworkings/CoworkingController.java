package com.mai.db_cw.coworkings;

import com.mai.db_cw.infrastructure.operation_storage.OperationStorage;
import com.mai.db_cw.coworkings.dto.CoworkingRequest;
import com.mai.db_cw.coworkings.dto.CoworkingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.mai.db_cw.infrastructure.utility.OperationUtility.responseEntityDependsOnOperationStatus;

@RestController
@RequestMapping("/api/machines")
@Slf4j
@RequiredArgsConstructor
public class CoworkingController {
    private final CoworkingService coworkingService;
    private final OperationStorage operationStorage;

    /**
     * получение всех машинок
     *
     * @return
     */
    @GetMapping("/get-all")
    public ResponseEntity<List<CoworkingResponse>> getAllMachines() {
        return ResponseEntity
                .ok(coworkingService.findAllCoworkings());
    }

    /**
     * асинхронный метод добавления новой машинки
     * доступен ток админу
     *
     * @param coworkingRequest
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<UUID> addMachine(@RequestBody CoworkingRequest coworkingRequest) {
        UUID randomId = operationStorage.addOperationReturningUUID();
        coworkingService.runAsyncCreateCoworking(randomId, coworkingRequest);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .header("Location", "/api/machines/status/" + randomId.toString())
                .body(randomId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status/{machineId}")
    public ResponseEntity<String> getReservationStatus(@PathVariable UUID machineId) {
        var operationStatus = operationStorage.getOperationStatus(machineId);
        return responseEntityDependsOnOperationStatus(operationStatus);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/del/{machineId}")
    public ResponseEntity<UUID> deleteMachine(@PathVariable UUID machineId) {
        operationStorage.addOperation(machineId);
        coworkingService.deleteAsyncById(machineId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(machineId);
    }
}
