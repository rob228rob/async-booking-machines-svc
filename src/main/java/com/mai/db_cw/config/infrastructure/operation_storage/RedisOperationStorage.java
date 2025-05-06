package com.mai.db_cw.config.infrastructure.operation_storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author batoyan.rl
 * @since 03.05.2025
 *
 * Redis-backed implementation of OperationStorage using inner POJO OperationRecord.
 * In Redis JSON: { "operationStatus": "ACCEPTED", "cause": ..., "httpStatus": ... }
 * In contract methods, OperationStatus enum is used.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisOperationStorage implements OperationStorage {
    private static final String HASH_KEY = "operations";
    private final HashOperations<String, String, OperationRecord> hashOps;

    @Override
    public UUID addOperationReturningUUID() {
        UUID id = UUID.randomUUID();
        OperationRecord rec = new OperationRecord(OperationStatus.ACCEPTED.name(), null, null);
        hashOps.put(HASH_KEY, id.toString(), rec);
        log.info("Registered new operation with id: {}", id);
        return id;
    }

    @Override
    public void updateOperationStatus(UUID operationId, OperationStatus status) {
        String key = operationId.toString();
        OperationRecord rec = hashOps.get(HASH_KEY, key);
        log.info("Fetched record for updateOperationStatus id {}: {}", key, rec);
        if (rec == null) {
            throw new IllegalArgumentException("Операция с ID " + key + " не найдена.");
        }
        rec.setOperationStatus(status.name());
        hashOps.put(HASH_KEY, key, rec);
        log.info("Updated operation status for id: {} to {}", key, status);
    }

    @Override
    public void failOperation(UUID operationId, String cause, HttpStatus httpStatus) {
        String key = operationId.toString();
        OperationRecord rec = hashOps.get(HASH_KEY, key);
        log.info("Fetched record for failOperation id {}: {}", key, rec);
        if (rec == null) {
            throw new IllegalArgumentException("Операция с ID " + key + " не найдена.");
        }
        rec.setOperationStatus(OperationStatus.FINISHED_UNSUCCESSFULLY.name());
        rec.setCause(cause);
        rec.setHttpStatus(httpStatus != null ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR);
        hashOps.put(HASH_KEY, key, rec);
        log.info("Marked operation as failed with id: {}; cause: {}", key, cause);
    }

    @Override
    public OperationStatus getOperationStatus(UUID operationId) {
        String key = operationId.toString();
        OperationRecord rec = hashOps.get(HASH_KEY, key);
        log.info("Fetched record for getOperationStatus id {}: {}", key, rec);
        if (rec == null) {
            log.info("Status undefined for id: {}", operationId);
            return OperationStatus.UNDEFINED;
        }
        OperationStatus status = OperationStatus.valueOf(rec.getOperationStatus());
        log.info("Retrieved status {} for id: {}", status, operationId);
        return status;
    }

    @Override
    public void removeOperation(UUID operationId) {
        String key = operationId.toString();
        hashOps.delete(HASH_KEY, key);
        log.info("Removed operation id: {}", key);
    }

    @Override
    public boolean containsOperation(UUID operationId) {
        String key = operationId.toString();
        boolean exists = hashOps.hasKey(HASH_KEY, key);
        log.info("Contains operation {}: {}", operationId, exists);
        return exists;
    }

    @Override
    public void addOperation(UUID operationId) {
        if (operationId == null) {
            throw new IllegalStateException("operation must not be null");
        }
        String key = operationId.toString();
        OperationRecord rec = new OperationRecord(OperationStatus.ACCEPTED.name(), null, null);
        hashOps.put(HASH_KEY, key, rec);
        log.info("Added operation id: {}", key);
    }

    @Override
    public void successfully(UUID operationId) {
        String key = operationId.toString();
        OperationRecord rec = hashOps.get(HASH_KEY, key);
        log.info("Fetched record for successfully id {}: {}", key, rec);
        if (rec == null) {
            throw new IllegalArgumentException("Операция с ID " + key + " не найдена.");
        }
        rec.setOperationStatus(OperationStatus.FINISHED_SUCCESSFULLY.name());
        hashOps.put(HASH_KEY, key, rec);
        log.info("Marked operation as successful: {}", key);
    }

    /**
     * Inner POJO for storing operation data in Redis;
     * operationStatus stored as String enum name.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationRecord implements Serializable {
        private String operationStatus;
        private String cause;
        private HttpStatus httpStatus;
    }
}