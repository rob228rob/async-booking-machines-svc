package com.mai.db_cw.coworkings;

import com.mai.db_cw.infrastructure.exceptions.ApplicationException;
import com.mai.db_cw.infrastructure.operation_storage.OperationStorage;
import com.mai.db_cw.coworkings.dao.CoworkingRepository;
import com.mai.db_cw.coworkings.dto.CoworkingRequest;
import com.mai.db_cw.coworkings.dto.CoworkingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.mai.db_cw.infrastructure.utility.ExceptionUtility.throwIfAnyObjectIsNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoworkingService {

    private final CoworkingRepository machineDao;
    private final OperationStorage operationStorage;

    public void saveMachine(Coworking coworking) {
        machineDao.saveMachine(coworking);
    }

    public List<CoworkingResponse> findAllMachines() {
        List<CoworkingResponse> machines = machineDao.findAllMachinesReturningDto();
        if (machines.isEmpty()) {
            log.error("No machines found");
            return Collections.emptyList();
        }

        return machines;
    }

    public Optional<Coworking> findById(UUID machineId) {
        return machineDao.findMachineById(machineId);
    }

    /**
     * асинхронное создание машинки
     *
     * @param randomId
     * @param coworkingRequest
     */
    @Async
    public void runAsyncCreateMachine(UUID randomId, CoworkingRequest coworkingRequest) {
        try {
            throwIfAnyObjectIsNull("Invalid args", randomId, coworkingRequest);


            Coworking coworking = Coworking.builder()
                    .id(randomId)
                    .name(coworkingRequest.name())
                    .machineTypeId(coworkingRequest.type())
                    .dormitoryId(coworkingRequest.dormitoryId())
                    .build();

            log.info("Async operation status: request sent to psql: id - {}", randomId);
            machineDao.saveMachine(coworking);
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

