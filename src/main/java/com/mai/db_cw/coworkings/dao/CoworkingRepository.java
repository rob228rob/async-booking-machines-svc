package com.mai.db_cw.coworkings.dao;

import com.mai.db_cw.coworkings.Coworking;
import com.mai.db_cw.coworkings.dto.CoworkingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@RequiredArgsConstructor
@Component
public class CoworkingRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    // Переименован из MachineRowMapper
    private static class CoworkingRowMapper implements RowMapper<Coworking> {
        @Override
        public Coworking mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            return Coworking.builder()
                    .id(rs.getObject("id", UUID.class))
                    .locationId(rs.getObject("location_id", UUID.class))          // Было dormitoryId
                    .coworkingTypeId(rs.getInt("coworking_type_id"))              // Было machineTypeId
                    .name(rs.getString("name"))
                    .creationTime(rs.getTimestamp("creation_time").toLocalDateTime())
                    .modifiedTime(rs.getTimestamp("modified_time").toLocalDateTime())
                    .build();
        }
    }

    // Переименован из saveMachine
    public void saveCoworking(Coworking coworking) {
        String sql = "INSERT INTO coworkings (id, location_id, coworking_type_id, name) " +
                "VALUES (:id, :locationId, :coworkingTypeId, :name)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", coworking.getId())
                .addValue("locationId", coworking.getLocationId())
                .addValue("coworkingTypeId", coworking.getCoworkingTypeId())
                .addValue("name", coworking.getName());

        jdbcTemplate.update(sql, params);
    }

    // Переименован из deleteMachineById
    public void deleteCoworkingById(UUID id) {
        String sql = "DELETE FROM coworkings WHERE id = :id";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        jdbcTemplate.update(sql, params);
    }

    // Переименован из findMachineById
    public Optional<Coworking> findCoworkingById(UUID id) {
        String sql = "SELECT * FROM coworkings WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);

        return jdbcTemplate.query(sql, params, new CoworkingRowMapper()).stream().findFirst();
    }

    // Переименован из findAllMachines
    public List<Coworking> findAllCoworkings() {
        String sql = "SELECT * FROM coworkings";

        return jdbcTemplate.query(sql, new CoworkingRowMapper()).stream().toList();
    }

    // Переименован из findAllMachinesReturningDto
    public List<CoworkingResponse> findAllCoworkingsReturningDto() {
        String sql = "SELECT " +
                "c.id AS coworking_id, " +
                "c.name AS coworking_name, " +
                "l.name AS location_name, " +
                "ct.name AS coworking_type_name " +
                "FROM coworkings c " +
                "LEFT JOIN coworking_types ct ON ct.id = c.coworking_type_id " +
                "LEFT JOIN locations l ON l.id = c.location_id";

        Map<String, Object> params = new HashMap<>();

        return jdbcTemplate.query(sql, params, (rs, cnt) -> CoworkingResponse.builder()
                .id(UUID.fromString(rs.getString("coworking_id")))
                .name(rs.getString("coworking_name"))
                .locationName(rs.getString("location_name"))        // Было dormitoryName
                .coworkingType(rs.getString("coworking_type_name")) // Было machineType
                .build()
        );
    }
}
