package com.mai.db_cw.reservation.dao;
import com.mai.db_cw.reservation.dto.ReservationUserResponse;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ReservationUserResponseRowMapper implements RowMapper<ReservationUserResponse> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @Override
    public ReservationUserResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        UUID reservationId = rs.getObject("reservation_id", UUID.class);
        UUID machineId = rs.getObject("machine_id", UUID.class);
        String machineName = rs.getString("machine_name");
        String dormitoryName = rs.getString("dormitory_name");
        // временно откажемся от дня недели
//        int dayOfWeekInt = rs.getInt("day_of_week");
//        String dayOfWeek = getDayOfWeek(dayOfWeekInt);
        String startTime = rs.getTime("start_time").toLocalTime().toString();
        String endTime = rs.getTime("end_time").toLocalTime().toString();
        String reservationTime = startTime + " - " + endTime;
        String reservationDate = rs.getTimestamp("res_date").toLocalDateTime().toLocalDate().format(DATE_FORMATTER);
        String status = rs.getString("status");

        return ReservationUserResponse.builder()
                .reservationId(reservationId)
                .machineId(machineId)
                .machineName(machineName)
                .dormitoryName(dormitoryName)
                .reservationDate(reservationDate)
                //.dayOfWeek(dayOfWeek)
                .reservationTime(reservationTime)
                .status(status)
                .build();
    }

    /**
     * Преобразование числа дня недели в строку
     *
     * @param dayOfWeek Число дня недели (1=Понедельник, 7=Воскресенье)
     * @return Название дня недели
     */
    private String getDayOfWeek(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Понедельник";
            case 2 -> "Вторник";
            case 3 -> "Среда";
            case 4 -> "Четверг";
            case 5 -> "Пятница";
            case 6 -> "Суббота";
            case 7 -> "Воскресенье";
            default -> "Неизвестный день";
        };
    }
}
