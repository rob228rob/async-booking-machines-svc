package com.mai.db_cw.config.infrastructure.operation_storage;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
* Интерфейс для хранения асинхронных операций
 * с возможностью отслеживания их статусов
 * Базовая имплементация
 * @author
 */
@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
public interface OperationStorage {
    UUID addOperationReturningUUID();
    void updateOperationStatus(UUID operationId, OperationStatus status);
    void failOperation(UUID operationId, String cause, HttpStatus httpStatus);
    OperationStatus getOperationStatus(UUID operationId);
    void removeOperation(UUID operationId);
    boolean containsOperation(UUID operationId);
    void addOperation(UUID operationId);
    void successfully(UUID operationId);
}

