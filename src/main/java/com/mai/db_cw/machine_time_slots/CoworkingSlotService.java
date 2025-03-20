package com.mai.db_cw.machine_time_slots;

import com.mai.db_cw.coworkings.Coworking;
import com.mai.db_cw.coworkings.CoworkingService;
import com.mai.db_cw.coworkings.dao.CoworkingRepository;
import com.mai.db_cw.infrastructure.exceptions.ApplicationException;
import com.mai.db_cw.location.LocationRepository;
import com.mai.db_cw.machine_time_slots.dto.MachineTimeSlotResponse;
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
 * Сервис для работы с пространствами и их временными слотами.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoworkingSlotService {

    private final MachineTimeSlotRepository coworkingTimeSlotRepository;
    private final TimeSlotRepository timeSlotRepository;          // Исправлено: вместо MachineTimeSlotRepository
    private final CoworkingRepository coworkingRepository;
    private final LocationRepository locationRepository;
    private final CoworkingService coworkingService;
    private final ReservationRepository reservationRepository;

    /**
     * Получает все коворкинги с их временными слотами (раньше: getAllMachinesWithTimeSlots).
     *
     * @return DTO со списком коворкингов и их временных слотов.
     */
    public MachineTimeSlotResponse getAllCoworkingsWithTimeSlots() {
        List<Coworking> coworkings = coworkingRepository.findAllCoworkings();
        if (coworkings.isEmpty()) {
            log.error("No coworkings found");
            return MachineTimeSlotResponse.builder()
                    .slots(Collections.emptyList())
                    .build();
        }

        // Все записи из таблицы machine_time_slots (переименованы поля в колонках).
        List<MachineTimeSlot> allCoworkingTimeSlots = coworkingTimeSlotRepository.findAllMachineTimeSlots();
        if (allCoworkingTimeSlots.isEmpty()) {
            log.error("No coworking time slots found");
            throw new ApplicationException("No coworking slots found, illegal state", HttpStatus.NOT_FOUND);
        }

        // Группируем временные слоты по coworkingId
        Map<UUID, List<MachineTimeSlot>> coworkingSlotsMap = allCoworkingTimeSlots.stream()
                .collect(Collectors.groupingBy(MachineTimeSlot::getCoworkingId));
        log.debug("Grouped slots map: {}", coworkingSlotsMap);

        // Собираем данные о каждом коворкинге
        var coworkingSlots = coworkings.stream()
                .map(cw -> {
                    List<MachineTimeSlot> cwTimeSlotList = coworkingSlotsMap
                            .getOrDefault(cw.getId(), Collections.emptyList());
                    log.debug("TimeSlot list for coworking {}: {}", cw.getName(), cwTimeSlotList);

                    // Получаем реальные данные из timeSlotRepository по каждому слоту
                    List<TimeSlotResponse> timeSlots = cwTimeSlotList.stream()
                            .map(mts -> {
                                TimeSlot ts = timeSlotRepository.findById(mts.getTimeSlotId())
                                        .orElseThrow(() -> new ApplicationException("TimeSlot not found", HttpStatus.NOT_FOUND));
                                return TimeSlotResponse.builder()
                                        .id(ts.getId())
                                        .dayOfWeek(ts.getDayOfWeek())
                                        .startTime(ts.getStartTime())
                                        .endTime(ts.getEndTime())
                                        .isAvailable(mts.getIsAvailable())  // Берём флаг из MachineTimeSlot
                                        .build();
                            }).toList();

                    // Находим «локацию» (бывший dormitory) для этого коворкинга
                    var location = locationRepository
                            .findDormitoryById(cw.getLocationId())
                            .orElseThrow(() -> new ApplicationException("Location not found", HttpStatus.NOT_FOUND));

                    return MachineTimeSlotResponse.TimeSlotsForSingleMachine.builder()
                            .machineId(cw.getId())         // Можно переименовать в coworkingId
                            .machineName(cw.getName())     // coworkingName
                            .dormitoryName(location.getName())      // locationName
                            .dormitoryAddress(location.getAddress())// locationAddress
                            .timeSlots(timeSlots)
                            .build();
                })
                .toList();

        return MachineTimeSlotResponse.builder()
                .slots(coworkingSlots)
                .build();
    }

    /**
     * Получаем временные слоты для конкретного коворкинга (раньше: getMachineSlots).
     */
    public MachineTimeSlotResponse getCoworkingSlots(UUID coworkingId, LocalDate startDate, int weeks) {
        Coworking coworking = coworkingService.findById(coworkingId)
                .orElseThrow(() -> new ApplicationException("Coworking not found", HttpStatus.NOT_FOUND));

        LocalDate endDate = startDate.plusWeeks(weeks);
        List<LocalDate> allDates = generateDates(startDate, endDate);

        // Генерируем временные слоты «на лету» (больше нет отдельной таблицы?).
        List<TimeSlotResponse> generatedSlots = generateTimeSlotsForPeriod(allDates);

        // Получаем список занятых бронирований для данного коворкинга
        List<Reservation> reservations = reservationRepository.findReservationsForCoworkingInPeriod(
                coworkingId, startDate, endDate);

        // Группируем по дате
        Map<LocalDate, List<Reservation>> reservationsByDate = reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getResDate));

        // Проставляем флаг занятости
        generatedSlots.forEach(slot -> {
            LocalDate slotDate = slot.getDate();
            List<Reservation> dailyReservations = reservationsByDate.getOrDefault(slotDate, Collections.emptyList());

            boolean isReserved = dailyReservations.stream()
                    .anyMatch(res -> slot.getStartTime().equals(res.getStartTime()) &&
                            slot.getEndTime().equals(res.getEndTime()));

            slot.setAvailable(!isReserved);
        });

        // Находим локацию
        var location = locationRepository
                .findDormitoryById(coworking.getLocationId())
                .orElseThrow(() -> new ApplicationException("Location not found", HttpStatus.NOT_FOUND));

        MachineTimeSlotResponse.TimeSlotsForSingleMachine cwSlots =
                MachineTimeSlotResponse.TimeSlotsForSingleMachine.builder()
                        .machineId(coworking.getId())
                        .machineName(coworking.getName())
                        .dormitoryName(String.valueOf(location.getName()))      // locationName
                        .dormitoryAddress(location.getAddress())
                        .timeSlots(generatedSlots)
                        .build();

        return MachineTimeSlotResponse.builder()
                .slots(List.of(cwSlots))
                .build();
    }

    /**
     * Генерирует список всех дат от startDate до endDate (включительно).
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
     * Генерирует «виртуальные» слоты по 1 часу, с 08:00 до 17:00
     * без хранения их в БД.
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
