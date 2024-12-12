package com.mai.db_cw.reservation;

import com.mai.db_cw.infrastructure.exceptions.ApplicationException;
import com.mai.db_cw.infrastructure.exceptions.EntityNotFoundException;
import com.mai.db_cw.infrastructure.exceptions.InvalidUserInfoException;
import com.mai.db_cw.infrastructure.utility.OperationStorage;
import com.mai.db_cw.machine_time_slots.MachineTimeSlotRepository;
import com.mai.db_cw.machines.Machine;
import com.mai.db_cw.machines.MachineService;
import com.mai.db_cw.reservation.dao.ReservationRepository;
import com.mai.db_cw.reservation.dto.ReservationRequest;
import com.mai.db_cw.reservation.dto.ReservationUserResponse;
import com.mai.db_cw.user.User;
import com.mai.db_cw.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MachineTimeSlotRepository machineTimeSlotRepository;
    private final UserService userService;
    private final MachineService machineService;
    private final OperationStorage operationStorage;

    /**
     * Метод для бронирования слота
     */
    @Async
    @Transactional
    public void bookReservation(UUID randomId, String userEmail, ReservationRequest request) {
        try {
            // Проверка существования пользователя и машинки
            User user = userService.findByEmail(userEmail)
                    .orElseThrow(() -> new InvalidUserInfoException("Пользователь не найден"));

            Machine machine = machineService.findById(request.machineId())
                    .orElseThrow(() -> new EntityNotFoundException("Машинка не найдена"));

            // Проверка доступности слота
            boolean isAvailable = reservationRepository
                    .findReservationsForMachineInPeriod(request.machineId(), request.resDate(), request.resDate())
                    .stream()
                    .noneMatch(reservation ->
                            (reservation.getStartTime().equals(request.startTime()) && reservation.getEndTime().equals(request.endTime())) ||
                                    (reservation.getStartTime().isBefore(request.endTime()) && reservation.getEndTime().isAfter(request.startTime()))
                    );

            if (request.resDate().isBefore(LocalDate.now()) && request.startTime().isBefore(LocalTime.now())) {
                throw new ApplicationException("Нельзя бронировать на время которое до сейчас, типа оно уже прошло ну, типа прошлое", HttpStatus.BAD_REQUEST);
            }

            if (!isAvailable) {
                throw new ApplicationException("Выбранный слот уже забронирован", HttpStatus.CONFLICT);
            }

            // Создание нового бронирования
            Reservation reservation = Reservation.builder()
                    .id(randomId)
                    .userId(user.getId())
                    .machineId(request.machineId())
                    .resDate(request.resDate())
                    .startTime(request.startTime())
                    .endTime(request.endTime())
                    .status("PENDING")
                    .creationTime(LocalDateTime.now())
                    .modifiedTime(LocalDateTime.now())
                    .build();

            reservationRepository.save(reservation);
            operationStorage.successfully(randomId);
        } catch (ApplicationException e) {
            operationStorage.failOperation(randomId, e.getMessage(), e.getHttpStatus());
        } catch (DataIntegrityViolationException e) {
            operationStorage.failOperation(randomId, e.getMessage(), HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            operationStorage.failOperation(randomId, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Кидает исключение если уже занят слот
     * Иначе отмечает в базе статус что слот занят, дабы избежать гонки за один и тот же слот
     *
     * @param machineId
     * @param timeSlotId
     */
    private void throwIfReservationAlreadyOccupied(UUID machineId, UUID timeSlotId) {
        var slot = machineTimeSlotRepository.findByIds(machineId, timeSlotId);
        if (slot.getIsAvailable()) {
            log.debug("Slot was occupied successful! Status isAvailable SET to FALSE");
            machineTimeSlotRepository.updateAvailability(machineId, timeSlotId, false);
            return;
        }

        log.error("Slot already occupied; Attempt failed");
        throw new ApplicationException("Slot already occupied!", HttpStatus.CONFLICT);
    }

    public List<Reservation> getAllReservationsByUserId(UUID userId) {
        return reservationRepository.findAllByUserId(userId);
    }

    public boolean checkReservationExist(UUID reservationId) {
        var reservation = reservationRepository.findById(reservationId);

        return reservation.isPresent();
    }

    /**
     * Поиск всех броней конкретного юзера
     *
     * @param email
     * @return
     */
    public List<ReservationUserResponse> getAllByUserMail(String email) {
        var user = userService.findByEmail(email);
        if (user.isEmpty()) {
            log.error("User by email not found");
            throw new ApplicationException("User by email not found: reservationService", HttpStatus.NOT_FOUND);
        }

        log.info("User by email found with email, id: {} {}", user.get().getEmail(), user.get().getId());
        return reservationRepository.findAllReservationsByUserId(user.get().getId());
    }

    /**
     * обновляет статусы асинхронно раз в 1 минуту(60000мс)
     */
    @Scheduled(fixedRate = 60000)
    public void updateBookingStatuses() {
        log.info("Начало работы метода обновления статусов на актуальные");
        LocalDateTime now = LocalDateTime.now();

        var reservations = reservationRepository.findAll().stream()
                .peek(reservation -> {
                    String newStatus = determineStatus(reservation, now);

                    // Если статус изменился, обновляем его
                    if (!reservation.getStatus().equals(newStatus)) {
                        reservation.setStatus(newStatus);
                        reservation.setModifiedTime(now);
                        reservationRepository.updateStatus(reservation);
                    }
                })
                .toList();
        log.info("обновлено статусов: {} {}", reservations.size(), reservations);
    }

    /**
     * Метод для определения нового статуса бронирования.
     */
    private String determineStatus(Reservation reservation, LocalDateTime now) {
        LocalDateTime startDateTime = LocalDateTime.of(reservation.getResDate(), reservation.getStartTime());
        LocalDateTime endDateTime = LocalDateTime.of(reservation.getResDate(), reservation.getEndTime());

        if (now.isBefore(startDateTime)) {
            return "PENDING"; // Если текущее время до начала бронирования
        } else if (now.isAfter(startDateTime) && now.isBefore(endDateTime)) {
            return "ACTIVE"; // Если текущее время в пределах временного слота
        } else {
            return "FINISHED"; // Если текущее время после окончания бронирования
        }
    }
}