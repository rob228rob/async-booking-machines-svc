package com.mai.db_cw.rest.machines_type;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MachineDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    Optional<MachineType> findMachineTypeById(int id) {
        String sql = "select * from machine_type where id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);

        return jdbcTemplate.query(sql, params, (rs, cnt) -> {
            var typeId = rs.getInt("id");
            var name = rs.getString("name");
            return new MachineType(typeId, name);
        }).stream().findFirst();

    }
}
