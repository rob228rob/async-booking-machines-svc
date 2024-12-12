package com.mai.db_cw.machines;

import com.mai.db_cw.infrastructure.exceptions.ApplicationException;
import com.mai.db_cw.infrastructure.utility.OperationStatus;
import com.mai.db_cw.infrastructure.utility.OperationStorage;
import com.mai.db_cw.machines.dao.MachineRepository;
import com.mai.db_cw.machines.dto.MachineRequest;
import com.mai.db_cw.machines.dto.MachineResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static com.mai.db_cw.infrastructure.utility.ExceptionUtility.throwIfAnyObjectIsNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineRepository machineDao;
    private final OperationStorage operationStorage;

    public void saveMachine(Machine machine) {
        machineDao.saveMachine(machine);
    }

    public List<MachineResponse> findAllMachines() {
        List<MachineResponse> machines = machineDao.findAllMachinesReturningDto();
        if (machines.isEmpty()) {
            log.error("No machines found");
            return Collections.emptyList();
        }

        return machines;
    }

    public Optional<Machine> findById(UUID machineId) {
        return machineDao.findMachineById(machineId);
    }

    /**
     * асинхронное создание машинки
     *
     * @param randomId
     * @param machineRequest
     */
    @Async
    public void runAsyncCreateMachine(UUID randomId, MachineRequest machineRequest) {
        try {
            throwIfAnyObjectIsNull("Invalid args", randomId, machineRequest);


            Machine machine = Machine.builder()
                    .id(randomId)
                    .name(machineRequest.name())
                    .machineTypeId(machineRequest.type())
                    .dormitoryId(machineRequest.dormitoryId())
                    .build();

            log.info("Async operation status: request sent to psql: id - {}", randomId);
            machineDao.saveMachine(machine);
            operationStorage.successfully(randomId);
        } catch (ApplicationException e) {
            operationStorage.failOperation(randomId, e.getMessage(), e.getHttpStatus());
        } catch (DataIntegrityViolationException e) {
            operationStorage.failOperation(randomId, e.getMessage(), HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            operationStorage.failOperation(randomId, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Async
    public void deleteAsyncById(UUID machineId) {
        try {
            log.info("delete async operation status: id - {}", machineId);
            machineDao.deleteMachineById(machineId);
            operationStorage.successfully(machineId);
        } catch (ApplicationException e) {
            operationStorage.failOperation(machineId, e.getMessage(), e.getHttpStatus());
        } catch (DataIntegrityViolationException e) {
            operationStorage.failOperation(machineId, e.getMessage(), HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            operationStorage.failOperation(machineId, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

