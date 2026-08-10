package com.mealsnow.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndFindsUserByEmail() {
        User user = new User();
        user.setEmail("test@meals.com");
        user.setPasswordHash("x".repeat(60));
        user.setRole(Role.CUSTOMER);
        // you write this
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@meals.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@meals.com");
        assertThat(found.get().getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(found.get().getId()).isNotNull();
    }
}