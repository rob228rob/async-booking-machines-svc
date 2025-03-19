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

    private final CoworkingRepository coworkingRepository;
    private final OperationStorage operationStorage;

    public void saveCoworking(Coworking coworking) {
        coworkingRepository.saveCoworking(coworking);
    }

    public List<CoworkingResponse> findAllCoworkings() {
        List<CoworkingResponse> coworkings = coworkingRepository.findAllCoworkingsReturningDto();
        if (coworkings.isEmpty()) {
            log.error("No coworkings found");
            return Collections.emptyList();
        }
        return coworkings;
    }

    public Optional<Coworking> findById(UUID coworkingId) {
        return coworkingRepository.findCoworkingById(coworkingId);
    }

    /**
     * Асинхронное создание коворкинга
     *
     * @param randomId         – Идентификатор коворкинга
     * @param coworkingRequest – DTO с данными для создания
     */
    @Async
    public void runAsyncCreateCoworking(UUID randomId, CoworkingRequest coworkingRequest) {
        try {
            throwIfAnyObjectIsNull("Invalid args", randomId, coworkingRequest);

            Coworking coworking = Coworking.builder()
                    .id(randomId)
                    .name(coworkingRequest.name())
                    .coworkingTypeId(coworkingRequest.type())
                    // Если в CoworkingRequest поле всё ещё называется dormitoryId(), нужно переименовать
                    .locationId(coworkingRequest.dormitoryId())
                    .build();

            log.info("Async operation status: request sent to psql: id - {}", randomId);
            coworkingRepository.saveCoworking(coworking);
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
    public void deleteAsyncById(UUID coworkingId) {
        try {
            log.info("Delete async operation status: id - {}", coworkingId);
            coworkingRepository.deleteCoworkingById(coworkingId);
            operationStorage.successfully(coworkingId);
        } catch (ApplicationException e) {
            operationStorage.failOperation(coworkingId, e.getMessage(), e.getHttpStatus());
        } catch (DataIntegrityViolationException e) {
            operationStorage.failOperation(coworkingId, e.getMessage(), HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            operationStorage.failOperation(coworkingId, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
