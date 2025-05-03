package com.mai.db_cw.rest.role.dao;

import com.mai.db_cw.rest.role.Role;
import com.mai.db_cw.rest.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleDao {

    Optional<Role> findById(int id);

    Optional<Role> findByName(String roleUser);

    void save(Role newRole);

    List<Role> findRolesByUserId(UUID id);

    List<Role> findRolesByEmail(String email);

    void saveUserRoles(User user, List<Role> adminRole);
}
