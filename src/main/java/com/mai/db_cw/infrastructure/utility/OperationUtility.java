package com.mai.db_cw.infrastructure.utility;

import com.mai.db_cw.infrastructure.operation_storage.OperationStatus;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 *  утилитный класс для упрощения взаимодействия
 *  с in-memory OperationStorage
 */
@UtilityClass
public class OperationUtility {

    /**
     * утилитный метод возвращающий ResponseEntity c разным
     * наполнением в зависимости от статуса операции асинхронной
     * @param operationStatus
     * @return
     */
    public static ResponseEntity<String> responseEntityDependsOnOperationStatus(
            OperationStatus operationStatus) {
        if (operationStatus == null) {
            return ResponseEntity.notFound().build();
        }

        return switch (operationStatus) {
            case ACCEPTED ->
                    ResponseEntity
                            .status(HttpStatus.ACCEPTED)
                            .body("Operation in progress");
            case FINISHED_SUCCESSFULLY ->
                    ResponseEntity
                            .status(HttpStatus.OK)
                            .body("Operation completed successfully");
            case FINISHED_UNSUCCESSFULLY ->
                    ResponseEntity
                            .status(operationStatus.getHttpStatus())
                            .body("Operation failed: " + operationStatus.getCause());
            case UNDEFINED -> ResponseEntity
                    .notFound().build();
        };
    }

}
