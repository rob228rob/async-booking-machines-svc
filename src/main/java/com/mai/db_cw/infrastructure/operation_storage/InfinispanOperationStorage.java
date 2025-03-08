package com.mai.db_cw.infrastructure.operation_storage;

import com.fasterxml.uuid.Generators;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Remote cache config to storing operations
 *
 * @author Batoyan Robert
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InfinispanOperationStorage implements OperationStorage {

    private final RemoteCache<UUID, OperationStatus> operationsCache;

    @Override
    public UUID addOperationReturningUUID() {
        UUID operationId = Generators.timeBasedEpochGenerator().generate();
        operationsCache.put(operationId, OperationStatus.ACCEPTED);
        log.debug("new operation register with id: {}", operationId);
        return operationId;
    }

    @Override
    public void updateOperationStatus(UUID operationId, OperationStatus status) {
        if (!operationsCache.containsKey(operationId)) {
            log.warn("operation id not found: {}", operationId);
            throw new IllegalArgumentException("Операция с ID " + operationId + " не найдена.");
        }
        log.debug("operation stastus updated from {} to {}", ((OperationStatus) operationsCache.get(operationId)).toString(), status);
        operationsCache.put(operationId, status);
    }

    @Override
    public void failOperation(UUID operationId, String cause, HttpStatus httpStatus) {
        if (!operationsCache.containsKey(operationId)) {
            log.warn("operation id not found: {}", operationId);
            throw new IllegalArgumentException("Операция с ID " + operationId + " не найдена.");
        }
        OperationStatus status = OperationStatus.FINISHED_UNSUCCESSFULLY;
        status.setFailureDetails(cause, httpStatus);
        operationsCache.put(operationId, status);
        log.info("fail operation register with id: {}", operationId);
    }

    @Override
    public OperationStatus getOperationStatus(UUID operationId) {
        OperationStatus status = operationsCache.get(operationId);
        if (status == null) {
            return OperationStatus.UNDEFINED;
        }
        log.info("get operation status with id: {}; status: {}", operationId, status);
        return status;
    }

    @Override
    public void removeOperation(UUID operationId) {
        log.info("remove operation id {}", operationId);
        operationsCache.remove(operationId);
    }

    @Override
    public boolean containsOperation(UUID operationId) {
        log.info("contains operation id {}", operationId);
        return operationsCache.containsKey(operationId);
    }

    @Override
    public void addOperation(UUID operationId) {
        if (operationId == null) {
            log.warn("operation id is null");
            throw new IllegalStateException("operation must not be null");
        }
        log.info("add operation id {}", operationId);
        operationsCache.put(operationId, OperationStatus.ACCEPTED);
    }

    @Override
    public void successfully(UUID operationId) {
        if (operationId == null) {
            throw new IllegalStateException("operation must not be null");
        }
        log.info("successfully operation id {}", operationId);
        operationsCache.put(operationId, OperationStatus.FINISHED_SUCCESSFULLY);
    }
}

