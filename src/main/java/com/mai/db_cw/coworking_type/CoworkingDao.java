package com.mai.db_cw.coworking_type;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CoworkingDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    Optional<CoworkingType> findMachineTypeById(int id) {
        String sql = "select * from coworking_type where id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);

        return jdbcTemplate.query(sql, params, (rs, cnt) -> {
            var typeId = rs.getInt("id");
            var name = rs.getString("name");
            return new CoworkingType(typeId, name);
        }).stream().findFirst();

    }
}
