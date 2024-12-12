package com.mai.db_cw.dormitory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DormitoryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<Dormitory> dormitoryRowMapper = (rs, i) ->
            Dormitory.builder()
                    .id(UUID.fromString(rs.getString("id")))
                    .address(rs.getString("address"))
                    .name(rs.getString("name"))
                    .creationTime(rs.getTimestamp("creation_time").toLocalDateTime())
                    .build();

    public Optional<Dormitory> findDormitoryById(UUID dormitoryId) {
        String sql = "select * from dormitories where id = :dormitoryId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("dormitoryId", dormitoryId);

        return jdbcTemplate.query(sql, params, dormitoryRowMapper).stream().findFirst();
    }

    /**
     *  Ищем все общаги с лимитом
     * @param limit
     * @return
     */
    public List<Dormitory> findAll(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        String sql = "select * from dormitories order by creation_time desc limit :limit";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit);

        return jdbcTemplate.query(sql, params, dormitoryRowMapper);
    }

    /**
     * обертка для поиска 10 общаг
     *
     * @return
     */
    public List<Dormitory> findAll() {
        log.info("find all dormitories method");
        return findAll(10);
    }

    @Async
    public void deleteAsync(UUID dormId) {
        log.info("delete dorm with id: {}", dormId);
        String sql = "delete from dormitories where id = :dormId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("dormId", dormId);

        jdbcTemplate.update(sql, params);
    }

    /**
     * Просто сохраняем новую общагу
     * @param build
     */
    @Async
    public void save(Dormitory build) {
        String sql = "insert into dormitories (id, name, address) values (:id, :name, :address)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", build.getId())
                .addValue("name", build.getName())
                .addValue("address", build.getAddress());

        jdbcTemplate.update(sql, params);
    }
}
