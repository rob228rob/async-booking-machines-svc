package com.mai.db_cw.reservation;

import com.mai.db_cw.infrastructure.exceptions.ApplicationException;
import com.mai.db_cw.infrastructure.exceptions.EntityNotFoundException;
import com.mai.db_cw.infrastructure.exceptions.InvalidUserInfoException;
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
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.UUID;

import static com.mai.db_cw.infrastructure.utility.ExceptionUtility.throwIfAnyObjectIsNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MachineTimeSlotRepository machineTimeSlotRepository;
    private final UserService userService;
    private final MachineService machineService;


    /**
     * Метод для бронирования слота
     */
    @Async
    @Transactional
    public void bookReservation(UUID randomId, String userEmail, ReservationRequest request) {
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
                .status("ACTIVE")
                .creationTime(LocalDateTime.now())
                .modifiedTime(LocalDateTime.now())
                .build();

        reservationRepository.save(reservation);
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
}