package com.mai.db_cw.reservation;

import com.mai.db_cw.config.infrastructure.operation_storage.OperationStorage;
import com.mai.db_cw.config.infrastructure.utility.OperationUtility;
import com.mai.db_cw.machines.dto.ReservationLog;
import com.mai.db_cw.reservation.dao.ReservationRepository;
import com.mai.db_cw.reservation.dto.ReservationRequest;
import com.mai.db_cw.reservation.dto.ReservationUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final OperationStorage operationStorage;

    /**
     * Эндпоинт для получения всех бронирований конкретного пользователя
     *
     * @param userId
     * @return List<Reservation> - список бронирований и статус 200. Иначе может лететь 404
     */
    @GetMapping("/all-by-user-id/{userId}/")
    public ResponseEntity<List<Reservation>> getReservationsByUserId(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(reservationService.getAllReservationsByUserId(userId));
    }

    /**
     * Эндпоинт для получения всех бронирований пользователя
     * который достается из контекста секьюрити через Principial
     *
     * @param principal
     * @return
     */
    @GetMapping("/current-user")
    public ResponseEntity<List<ReservationUserResponse>> getReservationsByUserId(
            Principal principal) {
        log.info("Request get all reservations by user email: {}", principal.getName());
        return ResponseEntity
                .ok(reservationService.getAllByUserMail(principal.getName()));
    }

    /**
     * Endpoint для создания нового бронирования асинхронно
     * Id операци по бронированию можно опрашивать по адресу:
     * /api/reservations/status/{reservationId}
     *
     * @param request Объект запроса на бронирование
     * @return ID бронирования и информация о том, как проверить статус
     */
    @PostMapping("/book")
    public ResponseEntity<Void> createReservation(
            @RequestBody ReservationRequest request,
            Principal principal) {

        UUID reservationId = operationStorage.addOperationReturningUUID();
        reservationService.bookReservation(reservationId, principal.getName(), request);
        String statusUrl = "/api/reservations/status/" + reservationId.toString();
        log.info("reservation request: reservId {}; userEmail {};", reservationId, principal.getName());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                // отдаем заголовом с url который можно опрашивать относительно статуса операции
                .header("Location", statusUrl)
                .build();
    }

    /**
     * Endpoint для проверки статуса бронирования по ID.
     *
     * @param reservationId ID бронирования
     * @return Статус 200 если добавление завершилось успешно
     * или 404 если не найдено
     * или HttpStatus который будет в объекте
     */
    @GetMapping("/status/{reservationId}")
    public ResponseEntity<String> getReservationStatus(@PathVariable UUID reservationId) {
        var operationStatus = operationStorage.getOperationStatus(reservationId);
        log.info("status checked operation on reservation with id {}, status {}", reservationId, operationStatus.toString());
        return OperationUtility.responseEntityDependsOnOperationStatus(operationStatus);
    }

    /**
     * Endpoint для получения логов
     *
     * @param limit количество последних логов
     * @return Статус 200, но если логов нет то пустой массив
     */
    @GetMapping("/get-logs")
    public ResponseEntity<List<ReservationLog>> getReservationLogs(
            @RequestParam Long limit) {
        if (limit == null) {
            return ResponseEntity
                    .ok(reservationRepository.findReservationLogs(50));
        }

        return ResponseEntity
                .ok(reservationRepository.findReservationLogs(limit));
    }

    @DeleteMapping("/del/{reservationId}")
    public ResponseEntity<Void> deleteReservation(
            @PathVariable UUID reservationId) {
        reservationRepository.deleteReservationById(reservationId);
        return ResponseEntity.noContent().build();
    }

}
