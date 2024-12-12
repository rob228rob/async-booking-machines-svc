package com.mai.db_cw.machines;

import com.mai.db_cw.infrastructure.exceptions.ApplicationException;
import com.mai.db_cw.machines.dao.MachineRepository;
import com.mai.db_cw.machines.dto.MachineRequest;
import com.mai.db_cw.machines.dto.MachineResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.mai.db_cw.infrastructure.utility.ExceptionUtility.throwIfAnyObjectIsNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineRepository machineDao;

    public void saveMachine(Machine machine) {
        machineDao.saveMachine(machine);
    }

    public List<MachineResponse> findAllMachines() {
        List<MachineResponse> machines = machineDao.findAllMachinesReturningDto();
        if (machines.isEmpty()) {
            log.error("No machines found");
            throw new ApplicationException("No machines found", HttpStatus.NOT_FOUND);
        }

        return machines;
    }

    public Optional<Machine> findById(UUID machineId) {
        return machineDao.findMachineById(machineId);
    }

    @Async
    public void runAsyncCreateMachine(UUID randomId, MachineRequest machineRequest) {
        throwIfAnyObjectIsNull("Invalid args", randomId, machineRequest);

        Machine machine = Machine.builder()
                .id(randomId)
                .name(machineRequest.name())
                .machineTypeId(machineRequest.type())
                .dormitoryId(machineRequest.dormitoryId())
                .build();

        log.info("Async operation status: request sent to psql: id - {}", randomId);
        machineDao.saveMachine(machine);
    }

    @Async
    public void deleteAsyncById(UUID machineId) {
        log.info("delete async operation status: id - {}", machineId);
        machineDao.deleteMachineById(machineId);
    }
}

