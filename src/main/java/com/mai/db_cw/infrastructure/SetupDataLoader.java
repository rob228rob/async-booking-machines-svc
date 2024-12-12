package com.mai.db_cw.infrastructure;

import com.fasterxml.uuid.Generators;
import com.mai.db_cw.role.dao.RoleDao;
import com.mai.db_cw.role.Role;
import com.mai.db_cw.user.dao.UserDao;
import com.mai.db_cw.user.User;
import com.mai.db_cw.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class SetupDataLoader implements
        ApplicationListener<ContextRefreshedEvent> {

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TransactionTemplate transactionTemplate;

    AtomicBoolean alreadySetup = new AtomicBoolean(false);

    @Value("${application.admin-data.email}")
    private String adminEmail;

    @Value("${application.admin-data.password}")
    private String adminPassword;

    @Transactional
    @Override
    public void onApplicationEvent(@NotNull ContextRefreshedEvent event) {

        if (alreadySetup.get()){
            return;
        }

        createRoleIfNotFound("ROLE_ADMIN");//, adminPrivileges);
        createRoleIfNotFound("ROLE_STAFF");//, Arrays.asList(lawyerPrivilege));
        createRoleIfNotFound("ROLE_USER");//, Arrays.asList(readPrivilege));

        var adminRole = roleDao.findByName("ROLE_ADMIN");
        var userRole = roleDao.findByName("ROLE_USER");
        if (adminRole.isEmpty() || userRole.isEmpty()) {
            log.error("admin | user roles is empty");
            return;
        }

        User user = new User();
        user.setId(Generators.timeBasedEpochGenerator().generate());
        user.setFirstName("Admin");
        user.setLastName("Account");
        user.setPassword(passwordEncoder.encode(String.valueOf(adminPassword)));
        user.setEmail(adminEmail);
        user.setEnabled(true);
        user.setTokenExpired(false);
        user.setCreationTime(LocalDateTime.now());
        user.setModifiedTime(LocalDateTime.now());

        if (!userService.existsByEmail(user.getEmail())) {
            userDao.save(user);
            roleDao.saveUserRoles(user, List.of(userRole.get(), adminRole.get()));

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            adminEmail,
                            adminPassword
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        alreadySetup.set(true);
    }

    Role createRoleIfNotFound(
            String name) {

        var role = roleDao.findByName(name);
        if (role.isEmpty()) {
            Role newRole = new Role();
            newRole.setName(name);
            //newRole.setPrivileges(privileges);
            roleDao.save(newRole);

            return newRole;
        }

        return role.get();
    }
}