package com.mai.db_cw.dormitory;

import com.fasterxml.uuid.Generators;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/dorm")
@RequiredArgsConstructor
public class DormController {

    private final DormitoryRepository dormitoryRepository;

    @GetMapping("/get-all")
    public ResponseEntity<List<Dormitory>> getAllDormitories() {
        return ResponseEntity
                .ok()
                .body(dormitoryRepository.findAll());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/del/{dormId}")
    public ResponseEntity<UUID> delDormitories(@PathVariable UUID dormId) {
        dormitoryRepository.deleteAsync(dormId);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(dormId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<UUID> delDormitories(
            @RequestParam String name,
            @RequestParam String address) {
        UUID randomId = Generators.timeBasedEpochGenerator().generate();
        dormitoryRepository.save(Dormitory
                .builder()
                .id(randomId)
                .name(name)
                .address(address)
                .build());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(randomId);
    }
}
