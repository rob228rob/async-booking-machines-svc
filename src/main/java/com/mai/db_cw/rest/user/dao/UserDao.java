package com.mai.db_cw.rest.user.dao;

import com.mai.db_cw.rest.user.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDao {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void save(User user);

    boolean deleteByEmail(String email);

    boolean deleteById(UUID userId);

    List<User> findAll();
}
