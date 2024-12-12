package com.mai.db_cw.infrastructure.utility;

import com.fasterxml.uuid.Generators;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * in-memory хранилище асинхронных операций с возможность
 * обновления их статусов
 * Скоп SINGLETON обеспечивает доступ к данным из различных потоков
 * в рантайме
 *
 * @author
 */
@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class OperationStorage {

    /**
     * общее хранилище операций
     */
    private final Map<UUID, OperationStatus> operations = new ConcurrentHashMap<>();

    /**
     * Добавление новой операции с ACCEPTED статусом.
     *
     * @return UUID новой операции
     */
    public UUID addOperationReturningUUID() {
        UUID operationId = Generators.timeBasedEpochGenerator().generate();
        operations.put(operationId, OperationStatus.ACCEPTED);
        return operationId;
    }

    /**
     * Обновление статуса операции.
     * @param operationId UUID операции.
     * @param status Новый статус операции.
     * @throws IllegalArgumentException если операция не найдена.
     */
    public void updateOperationStatus(UUID operationId, OperationStatus status) {
        if (!operations.containsKey(operationId)) {
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
    public void failOperation(UUID operationId, String cause, HttpStatus httpStatus) {
        if (!operations.containsKey(operationId)) {
            throw new IllegalArgumentException("Операция с ID " + operationId + " не найдена.");
        }
        OperationStatus status = OperationStatus.FINISHED_UNSUCCESSFULLY;
        status.setFailureDetails(cause, httpStatus);
        operations.put(operationId, status);
    }

    /**
     * Получение статуса операции.
     * @param operationId UUID операции.
     * @return Текущий статус операции.
     * @throws IllegalArgumentException если операция не найдена.
     */
    public OperationStatus getOperationStatus(UUID operationId) {
        OperationStatus status = operations.get(operationId);
        if (status == null) {
            return OperationStatus.UNDEFINED;
        }
        return status;
    }

    /**
     * Удаление операции из хранилища.
     * @param operationId UUID операции.
     */
    public void removeOperation(UUID operationId) {
        operations.remove(operationId);
    }

    /**
     * Проверка существования операции.
     * @param operationId UUID операции.
     * @return true, если операция существует.
     */
    public boolean containsOperation(UUID operationId) {
        return operations.containsKey(operationId);
    }

    public void addOperation(UUID operationId) {
        if (operationId == null) {
            throw new IllegalStateException("operation must not be null");
        }
        operations.put(operationId, OperationStatus.ACCEPTED);
    }

    public void successfully(UUID operationId) {
        if (operationId == null) {
            throw new IllegalStateException("operation must not be null");
        }

        operations.put(operationId, OperationStatus.FINISHED_SUCCESSFULLY);
    }
}

