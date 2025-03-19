package com.mai.db_cw.reservation.dao;

import com.mai.db_cw.infrastructure.exceptions.ApplicationException;
import com.mai.db_cw.infrastructure.operation_storage.OperationStorage;
import com.mai.db_cw.coworkings.dto.ReservationLog;
import com.mai.db_cw.reservation.Reservation;
import com.mai.db_cw.reservation.dto.ReservationUserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ReservationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Маппер сущности БД на POJO-класс Reservation
     */
    private static final RowMapper<Reservation> reservationRowMapper = (rs, cnt) -> Reservation.builder()
            .id(rs.getObject("id", UUID.class))
            .userId(rs.getObject("user_id", UUID.class))
            .coworkingId(rs.getObject("coworking_id", UUID.class))
            .resDate(rs.getDate("res_date").toLocalDate())
            .startTime(rs.getObject("start_time", LocalTime.class))
            .endTime(rs.getObject("end_time", LocalTime.class))
            .status(rs.getString("status"))
            .creationTime(rs.getTimestamp("creation_time").toLocalDateTime())
            .modifiedTime(rs.getTimestamp("modified_time").toLocalDateTime())
            .build();

    private final OperationStorage operationStorage;

    /**
     * Поиск всех бронирований пользователя
     *
     * @param userId UUID идентификатор пользователя
     * @return список бронирований
     */
    public List<Reservation> findAllByUserId(UUID userId) {
        String sql = "SELECT * FROM reservations WHERE user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        return jdbcTemplate.query(sql, params, reservationRowMapper);
    }

    /**
     * Сохранение нового бронирования
     *
     * @param reservation объект бронирования
     */
    public void save(Reservation reservation) {
        String sql = "INSERT INTO reservations " +
                "(id, user_id, coworking_id, res_date, start_time, end_time, status, creation_time, modified_time) " +
                "VALUES (:id, :userId, :coworkingId, :resDate, :startTime, :endTime, :status, :creationTime, :modifiedTime)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", reservation.getId())
                .addValue("userId", reservation.getUserId())
                .addValue("coworkingId", reservation.getCoworkingId())
                .addValue("resDate", reservation.getResDate())
                .addValue("startTime", reservation.getStartTime())
                .addValue("endTime", reservation.getEndTime())
                .addValue("status", reservation.getStatus())
                .addValue("creationTime", reservation.getCreationTime())
                .addValue("modifiedTime", reservation.getModifiedTime());

        jdbcTemplate.update(sql, params);
    }

    /**
     * Поиск бронирования по идентификатору
     *
     * @param id UUID идентификатор бронирования
     * @return Optional бронирования
     */
    public Optional<Reservation> findById(UUID id) {
        String sql = "SELECT * FROM reservations WHERE id = :id";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id);

        return jdbcTemplate.query(sql, params, reservationRowMapper).stream().findFirst();
    }

    /**
     * Обновление статуса бронирования
     *
     * @param reservationId UUID идентификатор бронирования
     * @param newStatus     новый статус
     */
    public void updateReservationStatus(UUID reservationId, String newStatus) {
        // Используется существующая процедура
        String sql = "CALL update_reservation_status(:res_id, :new_status)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("res_id", reservationId)
                .addValue("new_status", newStatus);

        jdbcTemplate.update(sql, params);
    }

    /**
     * Транзакционный метод для получения информации
     * о всех бронированиях пользователя
     *
     * @param userId UUID идентификатор пользователя
     * @return список ReservationUserResponse
     */
    @Transactional
    public List<ReservationUserResponse> findAllReservationsByUserId(UUID userId) {
        String sql = """
                SELECT 
                    r.id AS reservation_id,
                    c.id AS coworking_id,
                    c.name AS coworking_name,
                    l.name AS location_name,
                    r.res_date,
                    r.start_time,
                    r.end_time,
                    r.status,
                    r.creation_time,
                    r.modified_time
                FROM 
                    reservations r
                JOIN 
                    coworkings c ON r.coworking_id = c.id
                JOIN 
                    locations l ON c.location_id = l.id
                WHERE 
                    r.user_id = :userId
                ORDER BY 
                    r.res_date DESC,
                    r.start_time DESC,
                    r.creation_time DESC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);

        return jdbcTemplate.query(sql, params, new ReservationUserResponseRowMapper());
    }

    /**
     * Поиск бронирований для коворкинга в указанном периоде
     *
     * @param coworkingId UUID идентификатор коворкинга
     * @param startDate   начальная дата
     * @param endDate     конечная дата
     * @return список бронирований
     */
    public List<Reservation> findReservationsForCoworkingInPeriod(UUID coworkingId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT * FROM reservations " +
                "WHERE coworking_id = :coworkingId " +
                "AND res_date >= :startDate " +
                "AND res_date <= :endDate";

        Map<String, Object> params = new HashMap<>();
        params.put("coworkingId", coworkingId);
        params.put("startDate", startDate);
        params.put("endDate", endDate);

        return jdbcTemplate.query(sql, params, reservationRowMapper);
    }

    /**
     * Получение логов бронирований (с ограничением limit)
     *
     * @param limit максимальное кол-во записей
     * @return список ReservationLog
     */
    public List<ReservationLog> findReservationLogs(long limit) {
        String sql = "SELECT * FROM reservation_logs ORDER BY action_time DESC LIMIT :limit";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit);

        List<ReservationLog> query = jdbcTemplate.query(sql, params, (rs, c) -> ReservationLog.builder()
                .action(rs.getString("action"))
                .reservationId(UUID.fromString(rs.getString("reservation_id")))
                .oldData(rs.getString("old_data") != null ? rs.getString("old_data") : "")
                .newData(rs.getString("new_data") != null ? rs.getString("new_data") : "")
                .timestamp(rs.getTimestamp("action_time").toLocalDateTime())
                .build());

        if (query.isEmpty()) {
            return Collections.emptyList();
        }

        return query;
    }

    /**
     * Асинхронное удаление бронирования
     *
     * @param reservationId UUID идентификатор бронирования
     */
    @Async
    public void deleteReservationById(UUID reservationId) {
        try {
            String sql = "DELETE FROM reservations WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", reservationId);

            jdbcTemplate.update(sql, params);
            operationStorage.successfully(reservationId);
        } catch (ApplicationException e) {
            operationStorage.failOperation(reservationId, e.getMessage(), e.getHttpStatus());
        } catch (DataIntegrityViolationException e) {
            operationStorage.failOperation(reservationId, e.getMessage(), HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            operationStorage.failOperation(reservationId, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Подсчет всех бронирований
     *
     * @return кол-во записей в таблице reservations
     */
    public int count() {
        String sql = "SELECT COUNT(id) AS cnt FROM reservations";
        return jdbcTemplate.query(sql, Collections.emptyMap(), (rs, c) -> rs.getInt("cnt"))
                .stream()
                .findFirst()
                .orElse(0);
    }

    /**
     * Поиск всех бронирований со статусами ACTIVE или PENDING
     *
     * @return список бронирований
     */
    public List<Reservation> findAll() {
        String sql = "SELECT * FROM reservations " +
                "WHERE status LIKE '%ACTIVE%' " +
                "OR status LIKE '%PENDING%' " +
                "ORDER BY creation_time DESC";
        return jdbcTemplate.query(sql, reservationRowMapper);
    }

    /**
     * Обновление статуса и времени изменения бронирования
     *
     * @param reservation объект бронирования
     */
    public void updateStatus(Reservation reservation) {
        String sql = "UPDATE reservations " +
                "SET status = :status, " +
                "    modified_time = :modifiedTime " +
                "WHERE id = :id";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", reservation.getId())
                .addValue("status", reservation.getStatus())
                .addValue("modifiedTime", reservation.getModifiedTime());

        jdbcTemplate.update(sql, params);
    }
}
