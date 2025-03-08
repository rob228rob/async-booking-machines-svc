package com.mai.db_cw.infrastructure.operation_storage;

import com.fasterxml.uuid.Generators;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p> in-memory хранилище асинхронных операций с возможностью
 *  обновления их статусов </p>
 * <p>Скоп SINGLETON обеспечивает доступ к данным из различных потоков
 *  в рантайме, т.к. спринговый контекст обеспечивает гарантию
 *  единственности экземпляра бина<p/>
 */
@Slf4j
@Component
public class SimpleInMemoryOperationStorage implements OperationStorage {
    /**
     * common operation storage
     */
    private final Map<UUID, OperationStatus> operations = new ConcurrentHashMap<>();

    /**
     * Добавление новой операции с ACCEPTED статусом.
     *
     * @return UUID новой операции
     */
    @Override
    public UUID addOperationReturningUUID() {
        UUID operationId = Generators.timeBasedEpochGenerator().generate();
        operations.put(operationId, OperationStatus.ACCEPTED);
        log.info("new operation register with id: {}", operationId);
        return operationId;
    }

    /**
     * Обновление статуса операции.
     * @param operationId UUID операции.
     * @param status Новый статус операции.
     * @throws IllegalArgumentException если операция не найдена.
     */
    @Override
    public void updateOperationStatus(UUID operationId, OperationStatus status) {
        if (!operations.containsKey(operationId)) {
            log.warn("operation id not found: {}", operationId);
            throw new IllegalArgumentException("Операция с ID " + operationId + " не найдена.");
        }
        operations.put(operationId, status);
    }

    /**
     * Установка статуса ошибки для операции.
     * @param operationId UUID операции.
     * @param cause Причина ошибки.
     * @param httpStatus HTTP статус, связанный с ошибкой.
     * @throws IllegalArgumentException если операция не найдена.
     */
    @Override
    public void failOperation(UUID operationId, String cause, HttpStatus httpStatus) {
        if (!operations.containsKey(operationId)) {
            log.warn("operation id not found: {}", operationId);
            throw new IllegalArgumentException("Операция с ID " + operationId + " не найдена.");
        }
        OperationStatus status = OperationStatus.FINISHED_UNSUCCESSFULLY;
        status.setFailureDetails(cause, httpStatus);
        operations.put(operationId, status);
        log.info("fail operation register with id: {}", operationId);
    }

    /**
     * Получение статуса операции.
     * @param operationId UUID операции.
     * @return Текущий статус операции.
     * @throws IllegalArgumentException если операция не найдена.
     */
    @Override
    public OperationStatus getOperationStatus(UUID operationId) {
        OperationStatus status = operations.get(operationId);
        if (status == null) {
            return OperationStatus.UNDEFINED;
        }
        log.info("get operation status with id: {}; status: {}", operationId, status);
        return status;
    }

    /**
     * Удаление операции из хранилища.
     * @param operationId UUID операции.
     */
    @Override
    public void removeOperation(UUID operationId) {
        log.info("remove operation id {}", operationId);
        operations.remove(operationId);
    }

    /**
     * Проверка существования операции.
     * @param operationId UUID операции.
     * @return true, если операция существует.
     */
    @Override
    public boolean containsOperation(UUID operationId) {
        log.info("contains operation id {}", operationId);
        return operations.containsKey(operationId);
    }

    /**
     * Добавление операции с существующим айди, обычно может быть полезно
     * для операций удаления/обновления
     * @param operationId
     */
    @Override
    public void addOperation(UUID operationId) {
        if (operationId == null) {
            log.warn("operation id is null");
            throw new IllegalStateException("operation must not be null");
        }
        log.info("add operation id {}", operationId);
        operations.put(operationId, OperationStatus.ACCEPTED);
    }

    /**
     * выставляет статус операции как FINISHED_SUCCESSFULLY
     * @param operationId
     */
    @Override
    public void successfully(UUID operationId) {
        if (operationId == null) {
            throw new IllegalStateException("operation must not be null");
        }

        log.info("successfully operation id {}", operationId);
        operations.put(operationId, OperationStatus.FINISHED_SUCCESSFULLY);
    }
}
