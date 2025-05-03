package com.mai.db_cw.machine_time_slots;

import com.mai.db_cw.dormitory.DormitoryRepository;
import com.mai.db_cw.config.infrastructure.exceptions.ApplicationException;
import com.mai.db_cw.machine_time_slots.dto.MachineTimeSlotResponse;
import com.mai.db_cw.machines.Machine;
import com.mai.db_cw.machines.MachineService;
import com.mai.db_cw.machines.dao.MachineRepository;
import com.mai.db_cw.reservation.Reservation;
import com.mai.db_cw.reservation.dao.ReservationRepository;
import com.mai.db_cw.time_slot.TimeSlot;
import com.mai.db_cw.time_slot.TimeSlotRepository;
import com.mai.db_cw.time_slot.dto.TimeSlotResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для работы с машинами и их временными слотами.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MachineSlotService {

    private final MachineTimeSlotRepository machineSlotRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final MachineRepository machineRepository;
    private final DormitoryRepository dormitoryRepository;
    private final MachineService machineService;
    private final ReservationRepository reservationRepository;

    /**
     * Получает все машины с их временными слотами.
     *
     * @return DTO с информацией о машинах и их временных слотах.
     */
    public MachineTimeSlotResponse getAllMachinesWithTimeSlots() {
        List<Machine> machines = machineRepository.findAllMachines();
        if (machines.isEmpty()) {
            log.error("No machines found");
            return MachineTimeSlotResponse.builder()
                    .slots(Collections.emptyList())
                    .build();
        }

        List<MachineTimeSlot> allMachineTimeSlots = machineSlotRepository.findAllMachineTimeSlots();
        if (allMachineTimeSlots.isEmpty()) {
            log.error("No machine slots found");
            throw new ApplicationException("No machines slots found, illegal state", HttpStatus.NOT_FOUND);
        }

        // Группируем временные слоты по машине
        Map<UUID, List<MachineTimeSlot>> machineSlotsMap = allMachineTimeSlots.stream()
                .collect(Collectors.groupingBy(MachineTimeSlot::getMachineId));
        log.debug("Grouped by slots list: {}", machineSlotsMap);

        var machineSlots = machines
                .stream()
                .map(machine -> {
                    List<MachineTimeSlot> machineTimeSlotList = machineSlotsMap
                            .getOrDefault(machine.getId(), Collections.emptyList());
                    log.debug("machine Time Slot List: {}", machineTimeSlotList);
                    List<TimeSlotResponse> timeSlots = machineTimeSlotList
                            .stream()
                            .map(mts -> {
                                TimeSlot ts = timeSlotRepository.findById(mts.getTimeSlotId())
                                        .orElseThrow(() -> new ApplicationException("TimeSlot not found", HttpStatus.NOT_FOUND));
                                log.debug("time slot found by id: {}", mts.getTimeSlotId());
                                return TimeSlotResponse.builder()
                                        .id(ts.getId())
                                        .dayOfWeek(ts.getDayOfWeek())
                                        .startTime(ts.getStartTime())
                                        .endTime(ts.getEndTime())
                                        .isAvailable(mts.getIsAvailable())
                                        .build();
                            }).toList();

                    var dormitory = dormitoryRepository
                            .findDormitoryById(machine.getDormitoryId())
                            .orElseThrow(() -> new ApplicationException("Dormitory not found", HttpStatus.NOT_FOUND));

                    return MachineTimeSlotResponse.TimeSlotsForSingleMachine.builder()
                            .machineId(machine.getId())
                            .machineName(machine.getName())
                            .dormitoryName(dormitory.getName())
                            .dormitoryAddress(dormitory.getAddress())
                            .timeSlots(timeSlots)
                            .build();
                }).toList();

        return MachineTimeSlotResponse.builder()
                .slots(machineSlots)
                .build();
    }

    public MachineTimeSlotResponse getMachineSlots(UUID machineId, LocalDate startDate, int weeks) {
        // Получаем информацию о машине
        Machine machine = machineService.findById(machineId)
                .orElseThrow(() -> new ApplicationException("Machine not found", HttpStatus.NOT_FOUND));

        // Генерируем список дат на указанный период
        LocalDate endDate = startDate.plusWeeks(weeks);
        List<LocalDate> allDates = generateDates(startDate, endDate);

        // Генерируем временные слоты (например, каждый день с 08:00 до 17:00, шаг 1 час)
        List<TimeSlotResponse> generatedSlots = generateTimeSlotsForPeriod(allDates);

        // Получаем список занятых слотов из reservations для данного machineId и периода
        // Нам нужно проверить наличие бронирований.
        List<Reservation> reservations = reservationRepository.findReservationsForMachineInPeriod(machineId, startDate, endDate);

        Map<LocalDate, List<Reservation>> reservationsByDate = reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getResDate));

        generatedSlots.forEach(slot -> {
            // Находим бронирования для текущей даты слота
            List<Reservation> dailyReservations = reservationsByDate.getOrDefault(slot.getDate(), Collections.emptyList());

            // Проверяем, пересекается ли слот с каким-либо бронированием из reservations
            boolean isReserved = dailyReservations.stream()
                    .anyMatch(reservation ->
                            slot.getStartTime().equals(reservation.getStartTime()) &&
                                    slot.getEndTime().equals(reservation.getEndTime())
                    );

            // Обновляем доступность слота
            slot.setAvailable(!isReserved);
        });

        var dormitory = dormitoryRepository.findDormitoryById(machine.getDormitoryId()).orElseThrow();

        // Собираем ответ
        MachineTimeSlotResponse.TimeSlotsForSingleMachine machineSlots = MachineTimeSlotResponse.TimeSlotsForSingleMachine.builder()
                .machineId(machine.getId())
                .machineName(machine.getName())
                .dormitoryName(dormitory.getName())
                .dormitoryAddress(dormitory.getAddress())
                .timeSlots(generatedSlots)
                .build();

        return MachineTimeSlotResponse.builder()
                .slots(List.of(machineSlots))
                .build();
    }

    /**
     * генерирует нужные даты чтоб отдать тайм слоты на клиент
     *
     * @param startDate
     * @param endDate
     * @return
     */
    private List<LocalDate> generateDates(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }

    /**
     * функция для генерации слотов, в базе их не храним так как дорого
     *
     * @param allDates
     * @return
     */
    private List<TimeSlotResponse> generateTimeSlotsForPeriod(List<LocalDate> allDates) {
        List<TimeSlotResponse> slots = new ArrayList<>();
        for (LocalDate date : allDates) {
            for (LocalTime time = LocalTime.of(8, 0); time.isBefore(LocalTime.of(17, 0)); time = time.plusHours(1)) {
                slots.add(TimeSlotResponse.builder()
                        .date(date)
                        .dayOfWeek(date.getDayOfWeek().getValue())
                        .startTime(time)
                        .endTime(time.plusHours(1))
                        .isAvailable(true)
                        .build());
            }
        }
        return slots;
    }

}
