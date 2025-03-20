package com.mai.db_cw.machine_time_slots;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MachineTimeSlotRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Маппим поля:
     * coworking_id → MachineTimeSlot.coworkingId
     * time_slot_id → MachineTimeSlot.timeSlotId
     */
    private static final RowMapper<MachineTimeSlot> machineTSRowMapper = (rs, c) ->
            MachineTimeSlot.builder()
                    .coworkingId(rs.getObject("coworking_id", UUID.class))
                    .timeSlotId(rs.getObject("time_slot_id", UUID.class))
                    .isAvailable(rs.getBoolean("is_available"))
                    .creationTime(rs.getTimestamp("creation_time").toLocalDateTime())
                    .modifiedTime(rs.getTimestamp("modified_time").toLocalDateTime())
                    .build();

    /**
     * Вставляем новую запись в таблицу machine_time_slots (колонка coworking_id).
     */
    public void addMachineTimeSlot(MachineTimeSlot slot) {
        String sql = "INSERT INTO machine_time_slots (" +
                "coworking_id, time_slot_id, is_available, creation_time, modified_time" +
                ") VALUES (" +
                ":coworkingId, :timeSlotId, :isAvailable, :creationTime, :modifiedTime" +
                ")";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("coworkingId", slot.getCoworkingId())
                .addValue("timeSlotId", slot.getTimeSlotId())
                .addValue("isAvailable", slot.getIsAvailable())
                .addValue("creationTime", slot.getCreationTime())
                .addValue("modifiedTime", slot.getModifiedTime());

        jdbcTemplate.update(sql, params);
    }

    /**
     * Ищем конкретный слот (coworkingId + timeSlotId).
     */
    public MachineTimeSlot findByIds(UUID coworkingId, UUID timeSlotId) {
        String sql = "SELECT * FROM machine_time_slots " +
                "WHERE coworking_id = :coworkingId AND time_slot_id = :timeSlotId " +
                "ORDER BY creation_time DESC";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("coworkingId", coworkingId)
                .addValue("timeSlotId", timeSlotId);

        return jdbcTemplate.queryForObject(sql, params, machineTSRowMapper);
    }

    /**
     * Находим все слоты для конкретного коворкинга.
     */
    public List<MachineTimeSlot> findAllSlotsByCoworkingId(UUID coworkingId) {
        String sql = "SELECT * FROM machine_time_slots WHERE coworking_id = :coworkingId";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("coworkingId", coworkingId);

        return jdbcTemplate.query(sql, params, machineTSRowMapper);
    }

    /**
     * Обновление доступности конкретного слота.
     */
    public void updateAvailability(UUID coworkingId, UUID timeSlotId, Boolean isAvailable) {
        String sql = "UPDATE machine_time_slots " +
                "SET is_available = :isAvailable, modified_time = NOW() " +
                "WHERE coworking_id = :coworkingId AND time_slot_id = :timeSlotId";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("isAvailable", isAvailable)
                .addValue("coworkingId", coworkingId)
                .addValue("timeSlotId", timeSlotId);

        jdbcTemplate.update(sql, params);
    }

    /**
     * Находим все записи (используется в getAllCoworkingsWithTimeSlots).
     */
    public List<MachineTimeSlot> findAllMachineTimeSlots() {
        String sql = "SELECT * FROM machine_time_slots WHERE coworking_id IS NOT NULL";
        return jdbcTemplate.query(sql, machineTSRowMapper);
    }
}
