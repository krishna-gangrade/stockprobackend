package com.stockpro.auth.repository;

import com.stockpro.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("UserRepository Integration Tests")
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager entityManager;

    private User savedUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        savedUser = userRepository.save(User.builder()
                .fullName("Alice Smith")
                .email("alice@stockpro.com")
                .passwordHash("$2a$12$hashed")
                .role(User.Role.INVENTORY_MANAGER)
                .isActive(true)
                .build());
    }

    @Test
    @DisplayName("findByEmail returns user when present")
    void findByEmail_present_returnsUser() {
        Optional<User> result = userRepository.findByEmail("alice@stockpro.com");
        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("Alice Smith");
    }

    @Test
    @DisplayName("findByEmail returns empty when not present")
    void findByEmail_absent_returnsEmpty() {
        assertThat(userRepository.findByEmail("ghost@stockpro.com")).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail returns true for existing email")
    void existsByEmail_true() {
        assertThat(userRepository.existsByEmail("alice@stockpro.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail returns false for unknown email")
    void existsByEmail_false() {
        assertThat(userRepository.existsByEmail("nobody@stockpro.com")).isFalse();
    }

    @Test
    @DisplayName("findAllByRole returns only users with that role")
    void findAllByRole_returnsCorrectRole() {
        userRepository.save(User.builder()
                .fullName("Bob Jones").email("bob@stockpro.com")
                .passwordHash("hashed").role(User.Role.WAREHOUSE_STAFF).isActive(true).build());

        List<User> managers = userRepository.findAllByRole(User.Role.INVENTORY_MANAGER);
        assertThat(managers).hasSize(1);
        assertThat(managers.get(0).getEmail()).isEqualTo("alice@stockpro.com");
    }

    @Test
    @DisplayName("findByIsActive returns only active users")
    void findByIsActive_returnsActiveOnly() {
        userRepository.save(User.builder()
                .fullName("Inactive User").email("inactive@stockpro.com")
                .passwordHash("hashed").role(User.Role.WAREHOUSE_STAFF).isActive(false).build());

        List<User> active = userRepository.findByIsActive(true);
        assertThat(active).isNotEmpty().allMatch(User::getIsActive);
    }

    @Test
    @DisplayName("deactivateByUserId sets isActive=false")
    void deactivateByUserId_setsInactive() {
        int updated = userRepository.deactivateByUserId(savedUser.getUserId());
        assertThat(updated).isEqualTo(1);

        entityManager.flush();
        entityManager.clear();
        User reloaded = userRepository.findById(savedUser.getUserId()).orElseThrow();
        assertThat(reloaded.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("deactivateByUserId returns 0 for unknown ID")
    void deactivateByUserId_unknownId_returnsZero() {
        int updated = userRepository.deactivateByUserId(99999L);
        assertThat(updated).isZero();
    }



    @Test
    @DisplayName("countActiveByRole returns correct count")
    void countActiveByRole_correct() {
        long count = userRepository.countActiveByRole(User.Role.INVENTORY_MANAGER);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("save persists createdAt automatically")
    void save_persistsCreatedAt() {
        User user = userRepository.findById(savedUser.getUserId()).orElseThrow();
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("optimistic lock version starts at 0")
    void version_startsAtZero() {
        User user = userRepository.findById(savedUser.getUserId()).orElseThrow();
        assertThat(user.getVersion()).isZero();
    }
}
