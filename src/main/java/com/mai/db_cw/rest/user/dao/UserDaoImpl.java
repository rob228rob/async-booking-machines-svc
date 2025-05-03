package com.mai.db_cw.rest.user.dao;

import com.mai.db_cw.rest.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Repository
@RequiredArgsConstructor
public class UserDaoImpl implements UserDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final RowMapper<User> userRowMapper = new UserRowMapper();

    @Override
    public Optional<User> findById(UUID id) {
        String sql = "SELECT * FROM users WHERE id = :id";
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);

        List<User> results = namedParameterJdbcTemplate.query(sql, params, userRowMapper);
        return results.stream().findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = :email";
        Map<String, Object> params = new HashMap<>();
        params.put("email", email);

        List<User> results = namedParameterJdbcTemplate.query(sql, params, userRowMapper);
        return results.stream().findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = :email";
        Map<String, Object> params = new HashMap<>();
        params.put("email", email);

        Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public void save(User user) {
        String sql = "INSERT INTO users (id, first_name, last_name, email, password, enabled, token_expired, creation_time, modified_time) " +
                "VALUES (:id, :first_name, :last_name, :email, :password, :enabled, :token_expired, :creation_time, :modified_time)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", user.getId())
                .addValue("first_name", user.getFirstName())
                .addValue("last_name", user.getLastName())
                .addValue("email", user.getEmail())
                .addValue("password", user.getPassword())
                .addValue("enabled", user.isEnabled())
                .addValue("token_expired", user.isTokenExpired())
                .addValue("creation_time", user.getCreationTime())
                .addValue("modified_time", user.getModifiedTime());

        int rowsAffected = namedParameterJdbcTemplate.update(sql, params);

        if (rowsAffected != 1) {
            throw new DataAccessException("Failed to insert user into database") {};
        }
    }

    @Override
    public boolean deleteByEmail(String email) {
        String sql = "DELETE FROM users WHERE email = :email";
        Map<String, Object> params = new ConcurrentHashMap<>();
        params.put("email", email);

        return namedParameterJdbcTemplate.update(sql, params) > 0;
    }

    @Override
    public boolean deleteById(UUID id) {
        String sql = "DELETE FROM users WHERE id = :id";
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);

        return namedParameterJdbcTemplate.update(sql, params) > 0;
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        return namedParameterJdbcTemplate.query(sql, new HashMap<>(), userRowMapper);
    }

    // RowMapper для User
    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new User(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getBoolean("enabled"),
                    rs.getBoolean("token_expired"),
                    rs.getTimestamp("creation_time").toLocalDateTime(),
                    rs.getTimestamp("modified_time").toLocalDateTime()
            );
        }
    }
}