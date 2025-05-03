package com.mai.db_cw.rest.time_slot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TimeSlotRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<TimeSlot> timeSlotRowMapper = (rs, c) ->
            TimeSlot.builder()
                    .id(UUID.fromString(rs.getString("id")))
                    .dayOfWeek(rs.getInt("day_of_week"))
                    .startTime(rs.getTimestamp("start_time").toLocalDateTime().toLocalTime())
                    .endTime(rs.getTimestamp("end_time").toLocalDateTime().toLocalTime())
                    .build();

    public List<TimeSlot> findAllSlots() {
        String sql = "select * from time_slots";

        return jdbcTemplate.query(sql, timeSlotRowMapper).stream().toList();
    }

    public List<TimeSlot> findByDayOfWeek(int dayOfWeek) {
        String sql = "select * from time_slots where day_of_week = :dayOfWeek";
        MapSqlParameterSource params = new MapSqlParameterSource("dayOfWeek", dayOfWeek);

        return jdbcTemplate.query(sql, params, timeSlotRowMapper).stream().toList();
    }

    public void switchAvailableStatus(boolean isAvailable) {
        String sql = "update time_slots set is_available = :isAvailable where id = :id";
    }

    public Optional<TimeSlot> findById(UUID timeSlotId) {
        String sql = "select * from time_slots where id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", timeSlotId);
        return jdbcTemplate.query(sql, params, timeSlotRowMapper).stream().findFirst();
    }
}
