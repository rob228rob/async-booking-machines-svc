package com.mai.db_cw.dormitory;

import com.mai.db_cw.config.infrastructure.operation_storage.OperationStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.mai.db_cw.config.infrastructure.utility.OperationUtility.responseEntityDependsOnOperationStatus;

@Slf4j
@RestController
@RequestMapping("/api/dorm")
@RequiredArgsConstructor
public class DormController {

    private final DormitoryRepository dormitoryRepository;
    private final OperationStorage operationStorage;

    @GetMapping("/get-all")
    public ResponseEntity<List<Dormitory>> getAllDormitories() {
        return ResponseEntity
                .ok()
                .body(dormitoryRepository.findAll());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/del/{dormId}")
    public ResponseEntity<UUID> delDormitories(@PathVariable UUID dormId) {
        operationStorage.addOperation(dormId);
        dormitoryRepository.deleteAsync(dormId);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(dormId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<UUID> addNewDormitory(
            @RequestParam String name,
            @RequestParam String address) {
        UUID randomId = operationStorage.addOperationReturningUUID();
        dormitoryRepository.save(Dormitory
                .builder()
                .id(randomId)
                .name(name)
                .address(address)
                .build());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .header("Location", "/api/dorm/status/" + randomId.toString())
                .body(randomId);
    }

    @GetMapping("/status/{operationId}")
    public ResponseEntity<String> getOperationStatus(
            @PathVariable UUID operationId) {
        var operationStatus = operationStorage.getOperationStatus(operationId);

        return responseEntityDependsOnOperationStatus(operationStatus);
    }
}
