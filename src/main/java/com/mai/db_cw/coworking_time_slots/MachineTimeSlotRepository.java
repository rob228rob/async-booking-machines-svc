package com.mai.db_cw.coworking_time_slots;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MachineTimeSlotRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /** Статический RowMapper для маппинга ResultSet в объект MachineTimeSlot
    */
    private static final RowMapper<MachineTimeSlot> machineTSRowMapper = (rs, c) ->
            MachineTimeSlot.builder()
            .machineId(rs.getObject("machine_id", UUID.class))
            .timeSlotId(rs.getObject("time_slot_id", UUID.class))
            .isAvailable(rs.getBoolean("is_available"))
            .creationTime(rs.getTimestamp("creation_time").toLocalDateTime())
            .modifiedTime(rs.getTimestamp("modified_time").toLocalDateTime())
            .build();

    public void addMachineTimeSlot(MachineTimeSlot machineTimeSlot) {
        String sql = "INSERT INTO machine_time_slots (machine_id, time_slot_id, is_available, creation_time, modified_time) " +
                "VALUES (:machineId, :timeSlotId, :isAvailable, :creationTime, :modifiedTime)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("machineId", machineTimeSlot.getMachineId())
                .addValue("timeSlotId", machineTimeSlot.getTimeSlotId())
                .addValue("isAvailable", machineTimeSlot.getIsAvailable())
                .addValue("creationTime", machineTimeSlot.getCreationTime())
                .addValue("modifiedTime", machineTimeSlot.getModifiedTime());

        jdbcTemplate.update(sql, params);
    }

    public MachineTimeSlot findByIds(UUID machineId, UUID timeSlotId) {
        String sql = "SELECT * FROM machine_time_slots WHERE machine_id = :machineId AND time_slot_id = :timeSlotId order by creation_time desc";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("machineId", machineId)
                .addValue("timeSlotId", timeSlotId);

        return jdbcTemplate.queryForObject(sql, params, machineTSRowMapper);
    }

    public List<MachineTimeSlot> findAllSlotsByMachineId(UUID machineId) {
        String sql = "SELECT * FROM machine_time_slots WHERE machine_id = :machineId";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("machineId", machineId);

        return jdbcTemplate.query(sql, params, machineTSRowMapper);
    }

    public void updateAvailability(UUID machineId, UUID timeSlotId, Boolean isAvailable) {
        String sql = "UPDATE machine_time_slots SET is_available = :isAvailable, modified_time = NOW() " +
                "WHERE machine_id = :machineId AND time_slot_id = :timeSlotId";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("isAvailable", isAvailable)
                .addValue("machineId", machineId)
                .addValue("timeSlotId", timeSlotId);

        jdbcTemplate.update(sql, params);
    }

    public List<MachineTimeSlot> findAllMachineTimeSlots() {
        String sql = "select * from machine_time_slots where machine_id is not null";

        return jdbcTemplate.query(sql, machineTSRowMapper);
    }
}
