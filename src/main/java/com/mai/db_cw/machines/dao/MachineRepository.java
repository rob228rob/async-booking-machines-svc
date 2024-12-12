package com.mai.db_cw.machines.dao;

import com.mai.db_cw.machines.Machine;
import com.mai.db_cw.machines.dto.MachineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@RequiredArgsConstructor
@Component
public class MachineRepository{

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static class MachineRowMapper implements RowMapper<Machine> {
        @Override
        public Machine mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            return Machine.builder()
                    .id(rs.getObject("id", UUID.class))
                    .dormitoryId(rs.getObject("dormitory_id", UUID.class))
                    .machineTypeId(rs.getInt("machine_type_id"))
                    .name(rs.getString("name"))
                    .creationTime(rs.getTimestamp("creation_time").toLocalDateTime())
                    .modifiedTime(rs.getTimestamp("modified_time").toLocalDateTime())
                    .build();
        }
    }

    public void saveMachine(Machine machine) {
        String sql = "INSERT INTO machines (id, dormitory_id, machine_type_id, name, creation_time, modified_time) " +
                "VALUES (:id, :dormitoryId, :machineTypeId, :name, :creationTime, :modifiedTime)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", machine.getId())
                .addValue("dormitoryId", machine.getDormitoryId())
                .addValue("machineTypeId", machine.getMachineTypeId())
                .addValue("name", machine.getName())
                .addValue("creationTime", machine.getCreationTime())
                .addValue("modifiedTime", machine.getModifiedTime());

        jdbcTemplate.update(sql, params);
    }

    public void deleteMachineById(UUID id) {
        String sql = "DELETE FROM machines WHERE id = :id";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        jdbcTemplate.update(sql, params);
    }

    public Optional<Machine> findMachineById(UUID id) {
        String sql = "SELECT * FROM machines WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);

        return jdbcTemplate.query(sql, params, new MachineRowMapper()).stream().findFirst();
    }


    public List<Machine> findAllMachines() {
        String sql = "select * from machines";

        return jdbcTemplate.query(sql, new MachineRowMapper()).stream().toList();
    }

    public List<MachineResponse> findAllMachinesReturningDto() {
        String sql = "SELECT " +
                "m.id AS machine_id, " +
                "m.name AS machine_name, " +
                "d.name AS dormitory_name, " +
                "mt.name AS machine_type_name " +
                "FROM machines m " +
                "LEFT JOIN machine_types mt ON mt.id = m.machine_type_id " +
                "LEFT JOIN dormitories d ON d.id = m.dormitory_id";

        Map<String, Object> params = new HashMap<>();

        return jdbcTemplate.query(sql, params, (rs, cnt) -> {
            return MachineResponse.builder()
                    .id(UUID.fromString(rs.getString("machine_id")))
                    .name(rs.getString("machine_name"))
                    .dormitoryName(rs.getString("dormitory_name"))
                    .machineType(rs.getString("machine_type_name"))
                    .build();
        });
    }
}
