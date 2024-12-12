package com.mai.db_cw.role.dao;

import com.mai.db_cw.role.Role;
import com.mai.db_cw.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RoleDaoImpl implements RoleDao {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Role> roleRowMapper = roleRowMapper();

    @Override
    public Optional<Role> findById(int id) {
        String query = "select * from roles r where r.id = ?";

        return jdbcTemplate.query(query, roleRowMapper, id).stream().findFirst();
    }

    @Override
    public Optional<Role> findByName(String roleUser) {
        String query = "select * from roles r where r.name = ?";

        return jdbcTemplate.query(query, roleRowMapper, roleUser).stream().findFirst();
    }

    @Override
    public void save(Role newRole) {
        String query = """
            insert into roles (name) values (?)""";

        jdbcTemplate.update(query, newRole.getName());
    }

    @Override
    public List<Role> findRolesByUserId(UUID id) {
        String query = """
                SELECT r.id, r.name 
                FROM roles r 
                LEFT JOIN users_roles ur ON r.id = ur.role_id 
                WHERE ur.user_id = ?""";

        return jdbcTemplate.query(query, roleRowMapper, id);
    }

    @Override
    public List<Role> findRolesByEmail(String email) {
        String query = """
                SELECT r.id, r.name 
                FROM roles r 
                LEFT JOIN users_roles ur ON r.id = ur.role_id 
                LEFT JOIN users u ON u.id = ur.user_id
                WHERE u.email = ?""";

        return jdbcTemplate.query(query, roleRowMapper, email);
    }

    @Override
    public void saveUserRoles(User user, List<Role> roles) {
        String query = """
                insert into users_roles (user_id, role_id) values (?, ?)""";

        for (var role: roles) {
            jdbcTemplate.update(query, user.getId(), role.getId());
        }
    }

    private static RowMapper<Role> roleRowMapper() {
        return (rs, rowNum) -> new Role(
                rs.getInt("id"),
                rs.getString("name"));
    }

}
