package com.mai.db_cw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.*;

import com.mai.db_cw.user.dao.UserDao;
import com.mai.db_cw.user.dao.UserDaoImpl;
import com.mai.db_cw.user.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@Import(UserDaoImpl.class)
@ActiveProfiles("test")
public class UserDaoTest {

    @Autowired
    private UserDao userDao;

    private User testUser;

    @BeforeEach
    public void setUp() {
        testUser = new User(
                UUID.randomUUID(),
                "John",
                "Doe",
                "john.doe@example.com",
                "password",
                true,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        userDao.save(testUser);
    }

    @Test
    public void testFindById() {
        var userOptional = userDao.findById(testUser.getId());

        Assertions.assertTrue(userOptional.isPresent());

        var user = userOptional.get();
        Assertions.assertEquals(testUser.getId(), user.getId());
        Assertions.assertEquals(testUser.getPassword(), user.getPassword());
        Assertions.assertEquals(testUser.getFirstName(), user.getFirstName());
        Assertions.assertEquals(testUser.getLastName(), user.getLastName());
        Assertions.assertEquals(testUser.getEmail(), user.getEmail());
    }

    @Test
    public void testFindUserByFakeIdThrowingException() {
        UUID fakeId = UUID.randomUUID();

        var user = userDao.findById(fakeId);
        Assertions.assertTrue(user.isEmpty());
    }

    @Test
    public void testFindByEmail() {
        Optional<User> foundUser = userDao.findByEmail(testUser.getEmail());
        assertTrue(foundUser.isPresent());
        assertEquals(testUser.getId(), foundUser.get().getId());
    }

    @Test
    public void testExistsByEmail() {
        boolean exists = userDao.existsByEmail(testUser.getEmail());
        assertTrue(exists);
    }

    @Test
    public void testDeleteByEmail() {
        boolean deleted = userDao.deleteByEmail(testUser.getEmail());
        assertTrue(deleted);

        Optional<User> foundUser = userDao.findByEmail(testUser.getEmail());
        assertFalse(foundUser.isPresent());
    }

    @Test
    public void testDeleteById() {
        boolean deleted = userDao.deleteById(testUser.getId());
        assertTrue(deleted);

        Optional<User> foundUser = userDao.findById(testUser.getId());
        assertFalse(foundUser.isPresent());
    }
}