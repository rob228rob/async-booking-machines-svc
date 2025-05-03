package com.mai.db_cw.rest.user;

import com.fasterxml.uuid.Generators;
import com.mai.db_cw.config.infrastructure.exceptions.EntityNotFoundException;
import com.mai.db_cw.config.infrastructure.exceptions.UserNotFoundException;
import com.mai.db_cw.config.infrastructure.exceptions.UserRoleNotFoundException;
import com.mai.db_cw.rest.role.Role;
import com.mai.db_cw.rest.role.dao.RoleDao;

import com.mai.db_cw.rest.user.dao.UserDao;
import com.mai.db_cw.rest.user.dto.UpdateUserDto;
import com.mai.db_cw.rest.user.dto.UserRegistrationRequest;
import com.mai.db_cw.rest.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;

import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;

    private final RoleDao roleDao;

    private final ModelMapper modelMapper;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setModifiedTime(LocalDateTime.now());
        user.setCreationTime(LocalDateTime.now());
        userDao.save(user);
    }

    @Transactional
    public UserResponse saveUser(UserRegistrationRequest registerUserRequest) {
        registerUserRequest.validate();

        User user = saveDefaultUser(registerUserRequest);

        return modelMapper.map(user, UserResponse.class);
    }

    @Transactional
    public User saveUserWithRoles(UserRegistrationRequest registerUserRequest, List<String> roles) {
        var listRoles = roles.stream()
                .map(x ->
                        roleDao.findByName(x).orElseThrow(
                                () -> new EntityNotFoundException("Role " + x + " not found")))
                .toList();
        var user = saveDefaultUser(registerUserRequest, false);
        roleDao.saveUserRoles(user, listRoles);

        return user;
    }

    private User saveDefaultUser(UserRegistrationRequest registerUserRequest) {
        return saveDefaultUser(registerUserRequest, true);
    }

    private User saveDefaultUser(UserRegistrationRequest registerUserRequest, boolean withDefaultRole) {
        Role userRole = roleDao.findByName("ROLE_USER")
                .orElseThrow(() -> new UserRoleNotFoundException("Role with name ROLE_USER not found"));
        User user = modelMapper.map(registerUserRequest, User.class);

        user.setPassword(passwordEncoder.encode(registerUserRequest.getPassword()));
        user.setId(Generators.timeBasedEpochGenerator().generate());
        user.setEnabled(true);
        user.setTokenExpired(false);
        user.setCreationTime(LocalDateTime.now());
        user.setModifiedTime(LocalDateTime.now());
        userDao.save(user);
        if (withDefaultRole) {
            roleDao.saveUserRoles(user, List.of(userRole));
        }

        return user;
    }

    public User findUserByEmailForUserDetails(String email) {
        return userDao.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @Transactional
    public void upgradeUserToAdmin(UUID userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId.toString()));
        Role adminRole = roleDao.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new UserRoleNotFoundException("Role ROLE_ADMIN not found"));
        user.setEnabled(true);
        user.setModifiedTime(LocalDateTime.now());
        userDao.save(user);
    }

    /**
     * Простой поис юзера по емейлу
     *
     * @param email
     * @return User
     */
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isEmpty()) {
            return Optional.empty();
        }

        return userDao.findByEmail(email);
    }

    public UserResponse findUserByEmailReturningDto(String email) {
        var user = userDao.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with email not found: " + email));
        var roles = roleDao.findRolesByUserId(user.getId());

        UserResponse response = modelMapper.map(user, UserResponse.class);

        response.setRoles(roles.stream()
                .map(Role::getName)
                .toList());

        return response;
    }

    public boolean existsByEmail(String email) {
        return userDao.existsByEmail(email);
    }

    public User findUserById(UUID userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
    }

    public UserResponse findUserByIdReturningDto(UUID userId) {
        User user = findUserById(userId);
        return modelMapper.map(user, UserResponse.class);
    }

    public void deleteUserByUserId(UUID userId) {
        userDao.deleteById(userId);
    }

    @Transactional
    public void deleteCurrentUser(String email) {
        userDao.deleteByEmail(email);
    }

    public UserResponse updateUserPartly(UpdateUserDto userDto, String email) {
        Optional<User> byEmail = userDao.findByEmail(email);
        if (byEmail.isEmpty()) {
            throw new UserNotFoundException("User with email: " + email + " not found");
        }

        User user = byEmail.get();
        updateFieldIfNotNull(userDto.getEmail(), user::setEmail);
        updateFieldIfNotNull(userDto.getFirstName(), user::setFirstName);
        updateFieldIfNotNull(userDto.getLastName(), user::setLastName);
        user.setModifiedTime(LocalDateTime.now());

        userDao.save(user);
        return modelMapper.map(user, UserResponse.class);
    }

    public void updateUserPartly(UpdateUserDto userDto, UUID uuid) {
        Optional<User> userOptById = userDao.findById(uuid);
        if (userOptById.isEmpty()) {
            throw new UserNotFoundException("User with id: " + uuid + " not found");
        }

        User user = userOptById.get();
        updateFieldIfNotNull(userDto.getEmail(), user::setEmail);
        updateFieldIfNotNull(userDto.getFirstName(), user::setFirstName);
        updateFieldIfNotNull(userDto.getLastName(), user::setLastName);
        user.setModifiedTime(LocalDateTime.now());

        userDao.save(user);
    }

    private <T> void updateFieldIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    public Collection<User> findAll() {
        return userDao.findAll();
    }

    public UserResponse mapUserToResponse(User user) {
        return modelMapper.map(user, UserResponse.class);
    }

    public Boolean hasRoleLawyer(String email) {
        List<Role> roles = roleDao.findRolesByEmail(email);
        return roles.stream()
                .map(Role::getName)
                .toList()
                .contains("ROLE_LAWYER");
    }

    public List<UserResponse> findAllWithLimit(long limit) {
        List<User> users = userDao.findAll();
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        return users.stream()
                .map(x -> modelMapper.map(x, UserResponse.class))
                .toList();
    }
}

