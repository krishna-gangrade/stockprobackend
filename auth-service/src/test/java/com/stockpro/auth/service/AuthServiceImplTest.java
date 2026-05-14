package com.stockpro.auth.service;

import com.stockpro.auth.dto.UserMapper;
import com.stockpro.auth.dto.AuthResponse;
import com.stockpro.auth.dto.UserResponse;
import com.stockpro.auth.dto.TokenRefreshResponse;
import com.stockpro.auth.dto.request.AuthRequestDTOs.LoginRequest;
import com.stockpro.auth.dto.request.AuthRequestDTOs.RegisterRequest;
import com.stockpro.auth.dto.request.AuthRequestDTOs.RefreshTokenRequest;
import com.stockpro.auth.dto.request.AuthRequestDTOs.ChangePasswordRequest;
import com.stockpro.auth.dto.request.AuthRequestDTOs.ForgotPasswordRequest;
import com.stockpro.auth.dto.request.AuthRequestDTOs.ResetPasswordWithOtpRequest;
import com.stockpro.auth.dto.request.AuthRequestDTOs.UpdateProfileRequest;
import com.stockpro.auth.dto.request.AuthRequestDTOs.AdminUpdateUserRequest;
import com.stockpro.auth.entity.PasswordResetOtp;
import com.stockpro.auth.entity.User;
import com.stockpro.auth.exception.ApiException;
import com.stockpro.auth.exception.EmailAlreadyExistsException;
import com.stockpro.auth.exception.InvalidCredentialsException;
import com.stockpro.auth.exception.AccountDeactivatedException;
import com.stockpro.auth.exception.InvalidTokenException;
import com.stockpro.auth.exception.UserNotFoundException;
import com.stockpro.auth.exception.PasswordMismatchException;
import com.stockpro.auth.repository.AuditLogRepository;
import com.stockpro.auth.repository.PasswordResetOtpRepository;
import com.stockpro.auth.repository.UserRepository;
import com.stockpro.auth.service.impl.AuthServiceImpl;
import com.stockpro.auth.util.JwtUtil;
import com.stockpro.auth.util.RedisTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private PasswordResetOtpRepository passwordResetOtpRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RedisTokenStore redisTokenStore;
    @Mock private UserMapper userMapper;
    @Mock private PasswordResetMailService passwordResetMailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private UserResponse sampleUserResponse;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(1L)
                .fullName("Jane Doe")
                .email("jane@stockpro.com")
                .passwordHash("$2a$12$hashed")
                .role(User.Role.INVENTORY_MANAGER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .version(0L)
                .build();

        sampleUserResponse = UserResponse.builder()
                .userId(1L)
                .fullName("Jane Doe")
                .email("jane@stockpro.com")
                .role(User.Role.INVENTORY_MANAGER)
                .isActive(true)
                .build();

        ReflectionTestUtils.setField(authService, "otpLength", 6);
        ReflectionTestUtils.setField(authService, "otpExpiryMinutes", 10);
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("registers a new user successfully")
        void register_success() {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("Jane Doe");
            req.setEmail("jane@stockpro.com");
            req.setPassword("Secret@123");

            when(userRepository.existsByEmail("jane@stockpro.com")).thenReturn(false);
            when(passwordEncoder.encode("Secret@123")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);
            when(userMapper.toUserResponse(sampleUser)).thenReturn(sampleUserResponse);
            when(jwtUtil.generateAccessToken(any())).thenReturn("access-token");
            when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token");
            when(jwtUtil.getAccessTokenExpiryMs()).thenReturn(3600000L);

            AuthResponse result = authService.register(req, false);

            assertThat(result.getUser().getEmail()).isEqualTo("jane@stockpro.com");
            assertThat(result.getAccessToken()).isNotNull();
            verify(userRepository).save(any(User.class));
            verify(passwordResetMailService).sendWelcome("jane@stockpro.com", "Jane Doe");
            verify(auditLogRepository).save(any());
        }

        @Test
        @DisplayName("throws EmailAlreadyExistsException when email is taken")
        void register_duplicateEmail_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("jane@stockpro.com");
            req.setPassword("Secret@123");

            when(userRepository.existsByEmail("jane@stockpro.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req, false))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("jane@stockpro.com");
        }

        @Test
        @DisplayName("admin-created user uses the provided role")
        void register_adminCreating_usesProvidedRole() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("officer@stockpro.com");
            req.setPassword("Secret@123");
            req.setRole(User.Role.PURCHASE_OFFICER);

            User officerUser = User.builder()
                    .userId(2L).email("officer@stockpro.com")
                    .role(User.Role.PURCHASE_OFFICER).isActive(true).version(0L).build();
            
            UserResponse officerResp = UserResponse.builder()
                    .userId(2L).email("officer@stockpro.com")
                    .role(User.Role.PURCHASE_OFFICER).build();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(userRepository.save(any())).thenReturn(officerUser);
            when(userMapper.toUserResponse(officerUser)).thenReturn(officerResp);
            when(jwtUtil.generateAccessToken(officerUser)).thenReturn("token");
            when(jwtUtil.generateRefreshToken(officerUser)).thenReturn("refresh");
            when(jwtUtil.getAccessTokenExpiryMs()).thenReturn(28800000L);

            AuthResponse result = authService.register(req, true);

            assertThat(result.getUser().getRole()).isEqualTo(User.Role.PURCHASE_OFFICER);
            verify(passwordResetMailService).sendWelcome("officer@stockpro.com", null);
        }

        @Test
        @DisplayName("self-registration always assigns STAFF role regardless of request")
        void register_selfRegistration_alwaysStaff() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("hacker@stockpro.com");
            req.setPassword("Secret@123");
            req.setRole(User.Role.ADMIN); // attempt to escalate

            User staffUser = User.builder()
                    .userId(3L).email("hacker@stockpro.com")
                    .role(User.Role.WAREHOUSE_STAFF).isActive(true).version(0L).build();
            
            UserResponse staffResp = UserResponse.builder()
                    .userId(3L).email("hacker@stockpro.com")
                    .role(User.Role.WAREHOUSE_STAFF).build();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                assertThat(u.getRole()).isEqualTo(User.Role.WAREHOUSE_STAFF);
                return staffUser;
            });
            when(userMapper.toUserResponse(any())).thenReturn(staffResp);
            when(jwtUtil.generateAccessToken(staffUser)).thenReturn("token");
            when(jwtUtil.generateRefreshToken(staffUser)).thenReturn("refresh");
            when(jwtUtil.getAccessTokenExpiryMs()).thenReturn(28800000L);

            authService.register(req, false);

            verify(userRepository).save(argThat(u -> u.getRole() == User.Role.WAREHOUSE_STAFF));
            verify(passwordResetMailService).sendWelcome("hacker@stockpro.com", null);
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("returns AuthResponse with tokens on valid credentials")
        void login_success() {
            LoginRequest req = new LoginRequest();
            req.setEmail("jane@stockpro.com");
            req.setPassword("Secret@123");

            when(userRepository.findByEmail("jane@stockpro.com"))
                    .thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("Secret@123", sampleUser.getPasswordHash()))
                    .thenReturn(true);
            when(jwtUtil.generateAccessToken(sampleUser)).thenReturn("access-token");
            when(jwtUtil.generateRefreshToken(sampleUser)).thenReturn("refresh-token");
            when(jwtUtil.getAccessTokenExpiryMs()).thenReturn(28800000L);
            when(userMapper.toUserResponse(sampleUser)).thenReturn(sampleUserResponse);

            AuthResponse auth = authService.login(req);

            assertThat(auth.getAccessToken()).isEqualTo("access-token");
            assertThat(auth.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(auth.getTokenType()).isEqualTo("Bearer");
            assertThat(auth.getUser()).isNotNull();
            verify(redisTokenStore).storeRefreshToken(1L, "refresh-token");
            verify(redisTokenStore).storeSession("jane@stockpro.com", 1L);
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when user not found")
        void login_userNotFound_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail("ghost@stockpro.com");
            req.setPassword("whatever");

            when(userRepository.findByEmail("ghost@stockpro.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("throws InvalidCredentialsException on wrong password")
        void login_wrongPassword_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail("jane@stockpro.com");
            req.setPassword("wrong");

            when(userRepository.findByEmail("jane@stockpro.com"))
                    .thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("wrong", sampleUser.getPasswordHash()))
                    .thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("throws AccountDeactivatedException for inactive user")
        void login_inactiveUser_throws() {
            sampleUser.setIsActive(false);
            LoginRequest req = new LoginRequest();
            req.setEmail("jane@stockpro.com");
            req.setPassword("Secret@123");

            when(userRepository.findByEmail("jane@stockpro.com"))
                    .thenReturn(Optional.of(sampleUser));

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(AccountDeactivatedException.class);
        }
    }

    // ── Token refresh ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("issues a new access token for a valid refresh token")
        void refresh_success() {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("valid-refresh-token");

            when(jwtUtil.isTokenValid("valid-refresh-token")).thenReturn(true);
            when(jwtUtil.isRefreshToken("valid-refresh-token")).thenReturn(true);
            when(jwtUtil.extractUserId("valid-refresh-token")).thenReturn(1L);
            when(redisTokenStore.isRefreshTokenValid(1L, "valid-refresh-token")).thenReturn(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(jwtUtil.generateAccessToken(sampleUser)).thenReturn("new-access-token");
            when(jwtUtil.getAccessTokenExpiryMs()).thenReturn(28800000L);

            TokenRefreshResponse response = authService.refreshToken(req);

            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        }

        @Test
        @DisplayName("throws InvalidTokenException for expired refresh token")
        void refresh_expired_throws() {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("expired-token");

            when(jwtUtil.isTokenValid("expired-token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken(req))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("throws InvalidTokenException when token not in Redis (revoked)")
        void refresh_revoked_throws() {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("revoked-token");

            when(jwtUtil.isTokenValid("revoked-token")).thenReturn(true);
            when(jwtUtil.isRefreshToken("revoked-token")).thenReturn(true);
            when(jwtUtil.extractUserId("revoked-token")).thenReturn(1L);
            when(redisTokenStore.isRefreshTokenValid(1L, "revoked-token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken(req))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("revoked");
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("blacklists token and removes Redis entries")
        void logout_success() {
            when(jwtUtil.extractUserId("valid-token")).thenReturn(1L);
            when(jwtUtil.extractEmail("valid-token")).thenReturn("jane@stockpro.com");

            authService.logout("valid-token");

            verify(redisTokenStore).blacklistAccessToken("valid-token");
            verify(redisTokenStore).deleteRefreshToken(1L);
            verify(redisTokenStore).deleteSession("jane@stockpro.com");
        }

        @Test
        @DisplayName("swallows exception for malformed token gracefully")
        void logout_malformedToken_noException() {
            when(jwtUtil.extractUserId("bad-token"))
                    .thenThrow(new RuntimeException("Malformed JWT"));

            assertThatCode(() -> authService.logout("bad-token"))
                    .doesNotThrowAnyException();
        }
    }

    // ── Password change ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test
        @DisplayName("changes password and revokes session")
        void changePassword_success() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword("Old@1234");
            req.setNewPassword("New@5678");

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("Old@1234", sampleUser.getPasswordHash()))
                    .thenReturn(true);
            when(passwordEncoder.encode("New@5678")).thenReturn("newHash");
            when(userRepository.save(any())).thenReturn(sampleUser);

            authService.changePassword(1L, req);

            verify(redisTokenStore).deleteRefreshToken(1L);
            verify(redisTokenStore).deleteSession(sampleUser.getEmail());
            verify(auditLogRepository).save(any());
        }

        @Test
        @DisplayName("throws PasswordMismatchException when current password is wrong")
        void changePassword_wrongCurrent_throws() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword("Wrong@1234");
            req.setNewPassword("New@5678");

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("Wrong@1234", sampleUser.getPasswordHash()))
                    .thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(1L, req))
                    .isInstanceOf(PasswordMismatchException.class);
        }
    }

    @Nested
    @DisplayName("forgot/reset password OTP flow")
    class ForgotPasswordTests {

        @Test
        @DisplayName("sendForgotPasswordOtp stores OTP and sends email")
        void sendForgotPasswordOtp_success() {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("jane@stockpro.com");

            when(userRepository.findByEmail("jane@stockpro.com")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-otp");
            when(passwordResetOtpRepository.findAllByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc("jane@stockpro.com"))
                    .thenReturn(List.of());

            authService.sendForgotPasswordOtp(request);

            verify(passwordResetOtpRepository).save(argThat(otp ->
                    otp.getUser().equals(sampleUser)
                            && otp.getEmail().equals("jane@stockpro.com")
                            && otp.getOtpHash().equals("encoded-otp")
                            && !otp.isUsed()
            ));
            verify(passwordResetMailService).sendOtp(eq("jane@stockpro.com"), eq("Jane Doe"), anyString(), eq(10));
        }

        @Test
        @DisplayName("sendForgotPasswordOtp invalidates older unused OTPs before issuing a new one")
        void sendForgotPasswordOtp_invalidatesOlderOtps() {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("jane@stockpro.com");

            PasswordResetOtp olderOtp = PasswordResetOtp.builder()
                    .id(7L)
                    .user(sampleUser)
                    .email("jane@stockpro.com")
                    .otpHash("older-hash")
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .used(false)
                    .build();

            when(userRepository.findByEmail("jane@stockpro.com")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-otp");
            when(passwordResetOtpRepository.findAllByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc("jane@stockpro.com"))
                    .thenReturn(List.of(olderOtp));

            authService.sendForgotPasswordOtp(request);

            assertThat(olderOtp.isUsed()).isTrue();
            verify(passwordResetOtpRepository).saveAll(List.of(olderOtp));
            verify(passwordResetOtpRepository).save(argThat(otp ->
                    otp.getId() == null
                            && otp.getEmail().equals("jane@stockpro.com")
                            && otp.getOtpHash().equals("encoded-otp")
                            && !otp.isUsed()
            ));
        }

        @Test
        @DisplayName("sendForgotPasswordOtp rejects inactive account")
        void sendForgotPasswordOtp_inactiveAccount_throws() {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("jane@stockpro.com");
            sampleUser.setIsActive(false);

            when(userRepository.findByEmail("jane@stockpro.com")).thenReturn(Optional.of(sampleUser));

            assertThatThrownBy(() -> authService.sendForgotPasswordOtp(request))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("inactive");
        }

        @Test
        @DisplayName("resetPasswordWithOtp updates password, marks OTP used, and sends confirmation")
        void resetPasswordWithOtp_success() {
            ResetPasswordWithOtpRequest request = new ResetPasswordWithOtpRequest();
            request.setEmail("jane@stockpro.com");
            request.setOtp("123456");
            request.setNewPassword("New@5678");

            PasswordResetOtp resetOtp = PasswordResetOtp.builder()
                    .id(11L)
                    .user(sampleUser)
                    .email("jane@stockpro.com")
                    .otpHash("encoded-otp")
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .used(false)
                    .build();

            when(userRepository.findByEmail("jane@stockpro.com")).thenReturn(Optional.of(sampleUser));
            when(passwordResetOtpRepository.findAllByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc("jane@stockpro.com"))
                    .thenReturn(List.of(resetOtp));
            when(passwordEncoder.matches("123456", "encoded-otp")).thenReturn(true);
            when(passwordEncoder.encode("New@5678")).thenReturn("new-hash");

            authService.resetPasswordWithOtp(request);

            assertThat(resetOtp.isUsed()).isTrue();
            verify(userRepository).save(sampleUser);
            verify(passwordResetOtpRepository).saveAll(List.of(resetOtp));
            verify(redisTokenStore).deleteRefreshToken(1L);
            verify(redisTokenStore).deleteSession("jane@stockpro.com");
            verify(passwordResetMailService).sendResetSuccess("jane@stockpro.com", "Jane Doe");
        }

        @Test
        @DisplayName("resetPasswordWithOtp rejects expired OTP")
        void resetPasswordWithOtp_expiredOtp_throws() {
            ResetPasswordWithOtpRequest request = new ResetPasswordWithOtpRequest();
            request.setEmail("jane@stockpro.com");
            request.setOtp("123456");
            request.setNewPassword("New@5678");

            PasswordResetOtp resetOtp = PasswordResetOtp.builder()
                    .user(sampleUser)
                    .email("jane@stockpro.com")
                    .otpHash("encoded-otp")
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .used(false)
                    .build();

            when(userRepository.findByEmail("jane@stockpro.com")).thenReturn(Optional.of(sampleUser));
            when(passwordResetOtpRepository.findAllByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc("jane@stockpro.com"))
                    .thenReturn(List.of(resetOtp));

            assertThatThrownBy(() -> authService.resetPasswordWithOtp(request))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("expired");
        }
    }

    // ── Deactivate user ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("deactivateUser()")
    class DeactivateTests {

        @Test
        @DisplayName("deactivates user and revokes refresh token")
        void deactivate_success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(any())).thenReturn(sampleUser);

            authService.deactivateUser(1L, 99L);

            assertThat(sampleUser.getIsActive()).isFalse();
            verify(redisTokenStore).deleteRefreshToken(1L);
            verify(redisTokenStore).deleteSession(sampleUser.getEmail());
            verify(passwordResetMailService).sendAccountDeactivated("jane@stockpro.com", "Jane Doe");
        }

        @Test
        @DisplayName("deactivates user even if token cleanup fails")
        void deactivate_cleanupFailure_stillSucceeds() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(any())).thenReturn(sampleUser);
            doThrow(new RuntimeException("redis down")).when(redisTokenStore).deleteRefreshToken(1L);

            authService.deactivateUser(1L, 99L);

            assertThat(sampleUser.getIsActive()).isFalse();
            verify(userRepository).save(sampleUser);
            verify(passwordResetMailService).sendAccountDeactivated("jane@stockpro.com", "Jane Doe");
        }

        @Test
        @DisplayName("throws UserNotFoundException if user does not exist")
        void deactivate_notFound_throws() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.deactivateUser(99L, 1L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ── validateToken ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken returns false for blacklisted token")
    void validateToken_blacklisted_returnsFalse() {
        when(jwtUtil.isTokenValid("token")).thenReturn(true);
        when(redisTokenStore.isBlacklisted("token")).thenReturn(true);

        assertThat(authService.validateToken("token")).isFalse();
    }

    @Test
    @DisplayName("validateToken returns true for valid non-blacklisted token")
    void validateToken_valid_returnsTrue() {
        when(jwtUtil.isTokenValid("good-token")).thenReturn(true);
        when(redisTokenStore.isBlacklisted("good-token")).thenReturn(false);

        assertThat(authService.validateToken("good-token")).isTrue();
    }

    // ── getAllUsers ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllUsers returns mapped list")
    void getAllUsers_returnsList() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));
        when(userMapper.toUserResponseList(List.of(sampleUser)))
                .thenReturn(List.of(sampleUserResponse));

        List<UserResponse> result = authService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("jane@stockpro.com");
    }

    @Test
    @DisplayName("refreshFullAuth returns a fresh auth payload for a valid refresh token")
    void refreshFullAuth_validToken_returnsAuthResponse() {
        when(jwtUtil.isTokenValid("refresh-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtil.extractUserId("refresh-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(jwtUtil.generateAccessToken(sampleUser)).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken(sampleUser)).thenReturn("new-refresh");
        when(jwtUtil.getAccessTokenExpiryMs()).thenReturn(28800000L);
        when(userMapper.toUserResponse(sampleUser)).thenReturn(sampleUserResponse);

        AuthResponse response = authService.refreshFullAuth("refresh-token");

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        verify(redisTokenStore).storeRefreshToken(1L, "new-refresh");
        verify(redisTokenStore).storeSession("jane@stockpro.com", 1L);
    }

    @Test
    @DisplayName("refreshFullAuth rejects invalid refresh tokens")
    void refreshFullAuth_invalidToken_throws() {
        when(jwtUtil.isTokenValid("bad-refresh")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshFullAuth("bad-refresh"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    @DisplayName("getUserById returns mapped user response")
    void getUserById_returnsMappedUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userMapper.toUserResponse(sampleUser)).thenReturn(sampleUserResponse);

        UserResponse response = authService.getUserById(1L);

        assertThat(response.getEmail()).isEqualTo("jane@stockpro.com");
    }

    @Test
    @DisplayName("getUserByEmail normalizes the email before lookup")
    void getUserByEmail_normalizesEmail() {
        when(userRepository.findByEmail("jane@stockpro.com")).thenReturn(Optional.of(sampleUser));
        when(userMapper.toUserResponse(sampleUser)).thenReturn(sampleUserResponse);

        UserResponse response = authService.getUserByEmail("JANE@STOCKPRO.COM");

        assertThat(response.getUserId()).isEqualTo(1L);
        verify(userRepository).findByEmail("jane@stockpro.com");
    }

    @Test
    @DisplayName("getUsersByRole returns mapped list")
    void getUsersByRole_returnsMappedUsers() {
        when(userRepository.findAllByRole(User.Role.INVENTORY_MANAGER)).thenReturn(List.of(sampleUser));
        when(userMapper.toUserResponseList(List.of(sampleUser))).thenReturn(List.of(sampleUserResponse));

        List<UserResponse> response = authService.getUsersByRole(User.Role.INVENTORY_MANAGER);

        assertThat(response).extracting(UserResponse::getRole)
                .containsExactly(User.Role.INVENTORY_MANAGER);
    }

    @Test
    @DisplayName("updateProfile updates only non-null fields")
    void updateProfile_updatesProvidedFields() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Jane Updated");
        request.setPhone("9999999999");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return UserResponse.builder()
                    .userId(user.getUserId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .isActive(user.getIsActive())
                    .phone(user.getPhone())
                    .build();
        });

        UserResponse response = authService.updateProfile(1L, request);

        assertThat(response.getFullName()).isEqualTo("Jane Updated");
        assertThat(response.getPhone()).isEqualTo("9999999999");
    }

    @Test
    @DisplayName("adminUpdateUser updates fields and records an audit")
    void adminUpdateUser_updatesUser() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setFullName("Officer Jane");
        request.setPhone("8888888888");
        request.setRole(User.Role.PURCHASE_OFFICER);
        request.setIsActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return UserResponse.builder()
                    .userId(user.getUserId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .isActive(user.getIsActive())
                    .phone(user.getPhone())
                    .build();
        });

        UserResponse response = authService.adminUpdateUser(1L, request, 99L);

        assertThat(response.getFullName()).isEqualTo("Officer Jane");
        assertThat(response.getPhone()).isEqualTo("8888888888");
        assertThat(response.getRole()).isEqualTo(User.Role.PURCHASE_OFFICER);
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("adminUpdateUser sends reactivation email when an inactive user is restored")
    void adminUpdateUser_reactivation_sendsMail() {
        sampleUser.setIsActive(false);

        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setIsActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(sampleUserResponse);

        authService.adminUpdateUser(1L, request, 99L);

        verify(passwordResetMailService).sendAccountReactivated("jane@stockpro.com", "Jane Doe");
    }

    @Test
    @DisplayName("adminUpdateUser sends deactivation email when an active user is disabled")
    void adminUpdateUser_deactivation_sendsMail() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setIsActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(sampleUserResponse);

        authService.adminUpdateUser(1L, request, 99L);

        verify(passwordResetMailService).sendAccountDeactivated("jane@stockpro.com", "Jane Doe");
    }

    @Test
    @DisplayName("adminUpdateUser rejects unsupported managed roles")
    void adminUpdateUser_invalidRole_throws() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setRole(User.Role.SYSTEM);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.adminUpdateUser(1L, request, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported role");
    }

    @Test
    @DisplayName("adminUpdateUser prevents removing the last active admin")
    void adminUpdateUser_lastActiveAdmin_throws() {
        User adminUser = User.builder()
                .userId(10L)
                .fullName("Admin User")
                .email("admin@stockpro.com")
                .passwordHash("hash")
                .role(User.Role.ADMIN)
                .isActive(true)
                .build();

        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setIsActive(false);

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findAll()).thenReturn(List.of(adminUser));

        assertThatThrownBy(() -> authService.adminUpdateUser(10L, request, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least one active admin");
    }

    @Test
    @DisplayName("deactivateUser prevents deactivating the last active admin")
    void deactivate_lastActiveAdmin_throws() {
        User adminUser = User.builder()
                .userId(10L)
                .fullName("Admin User")
                .email("admin@stockpro.com")
                .passwordHash("hash")
                .role(User.Role.ADMIN)
                .isActive(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findAll()).thenReturn(List.of(adminUser));

        assertThatThrownBy(() -> authService.deactivateUser(10L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least one active admin");
    }

    @Test
    @DisplayName("sendForgotPasswordOtp rejects unknown email")
    void sendForgotPasswordOtp_userMissing_throws() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(" missing@stockpro.com ");

        when(userRepository.findByEmail("missing@stockpro.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.sendForgotPasswordOtp(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No active account found");
    }

    @Test
    @DisplayName("resetPasswordWithOtp rejects invalid OTP value")
    void resetPasswordWithOtp_invalidOtp_throws() {
        ResetPasswordWithOtpRequest request = new ResetPasswordWithOtpRequest();
        request.setEmail("jane@stockpro.com");
        request.setOtp("000000");
        request.setNewPassword("New@5678");

        PasswordResetOtp resetOtp = PasswordResetOtp.builder()
                .user(sampleUser)
                .email("jane@stockpro.com")
                .otpHash("encoded-otp")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();

        when(userRepository.findByEmail("jane@stockpro.com")).thenReturn(Optional.of(sampleUser));
        when(passwordResetOtpRepository.findAllByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc("jane@stockpro.com"))
                .thenReturn(List.of(resetOtp));
        when(passwordEncoder.matches("000000", "encoded-otp")).thenReturn(false);

        assertThatThrownBy(() -> authService.resetPasswordWithOtp(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid email or OTP");
    }

    @Test
    @DisplayName("resetPasswordWithOtp accepts a matching valid OTP even if another pending OTP exists")
    void resetPasswordWithOtp_matchesAnyValidPendingOtp() {
        ResetPasswordWithOtpRequest request = new ResetPasswordWithOtpRequest();
        request.setEmail("jane@stockpro.com");
        request.setOtp("121858");
        request.setNewPassword("New@5678");

        PasswordResetOtp latestWrongOtp = PasswordResetOtp.builder()
                .id(20L)
                .user(sampleUser)
                .email("jane@stockpro.com")
                .otpHash("latest-hash")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();

        PasswordResetOtp matchingOtp = PasswordResetOtp.builder()
                .id(21L)
                .user(sampleUser)
                .email("jane@stockpro.com")
                .otpHash("matching-hash")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();

        when(userRepository.findByEmail("jane@stockpro.com")).thenReturn(Optional.of(sampleUser));
        when(passwordResetOtpRepository.findAllByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc("jane@stockpro.com"))
                .thenReturn(List.of(latestWrongOtp, matchingOtp));
        when(passwordEncoder.matches("121858", "latest-hash")).thenReturn(false);
        when(passwordEncoder.matches("121858", "matching-hash")).thenReturn(true);
        when(passwordEncoder.encode("New@5678")).thenReturn("new-hash");

        authService.resetPasswordWithOtp(request);

        assertThat(latestWrongOtp.isUsed()).isTrue();
        assertThat(matchingOtp.isUsed()).isTrue();
        verify(passwordResetOtpRepository).saveAll(List.of(latestWrongOtp, matchingOtp));
    }
}
