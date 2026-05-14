package com.stockpro.auth.config;

import com.stockpro.auth.entity.User;
import com.stockpro.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BootstrapAdminInitializer unit tests")
class BootstrapAdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private BootstrapAdminInitializer initializer;
    private final ApplicationArguments args = new DefaultApplicationArguments(new String[0]);

    @BeforeEach
    void setUp() {
        initializer = new BootstrapAdminInitializer(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(initializer, "bootstrapAdminName", "StockPro Admin");
        ReflectionTestUtils.setField(initializer, "bootstrapAdminEmail", "ADMIN@STOCKPRO.LOCAL");
        ReflectionTestUtils.setField(initializer, "bootstrapAdminPassword", "Admin@123");
    }

    @Test
    void bootstrapAdminRunner_createsAdminWhenMissing() throws Exception {
        when(userRepository.existsByRole(User.Role.ADMIN)).thenReturn(false);
        when(passwordEncoder.encode("Admin@123")).thenReturn("encoded-password");

        initializer.bootstrapAdminRunner().run(args);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@stockpro.local");
        assertThat(saved.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    void bootstrapAdminRunner_skipsWhenDisabled() throws Exception {
        ReflectionTestUtils.setField(initializer, "bootstrapEnabled", false);

        initializer.bootstrapAdminRunner().run(args);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void bootstrapAdminRunner_skipsWhenAdminAlreadyExists() throws Exception {
        when(userRepository.existsByRole(User.Role.ADMIN)).thenReturn(true);

        initializer.bootstrapAdminRunner().run(args);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void bootstrapAdminRunner_skipsWhenPasswordTooShort() throws Exception {
        when(userRepository.existsByRole(User.Role.ADMIN)).thenReturn(false);
        ReflectionTestUtils.setField(initializer, "bootstrapAdminPassword", "short");

        initializer.bootstrapAdminRunner().run(args);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void bootstrapAdminRunner_skipsWhenPasswordBlank() throws Exception {
        when(userRepository.existsByRole(User.Role.ADMIN)).thenReturn(false);
        ReflectionTestUtils.setField(initializer, "bootstrapAdminPassword", " ");

        initializer.bootstrapAdminRunner().run(args);

        verify(userRepository, never()).save(any(User.class));
    }
}
