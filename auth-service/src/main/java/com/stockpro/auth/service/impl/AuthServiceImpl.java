package com.stockpro.auth.service.impl;

// Refreshed package state to resolve IDE sync issues
import com.stockpro.auth.dto.UserMapper;
import static com.stockpro.auth.dto.request.AuthRequestDTOs.*;
import com.stockpro.auth.dto.AuthResponse;
import com.stockpro.auth.dto.UserResponse;
import com.stockpro.auth.dto.TokenRefreshResponse;
import com.stockpro.auth.entity.AuthAuditLog;
import com.stockpro.auth.entity.PasswordResetOtp;
import com.stockpro.auth.entity.User;
import com.stockpro.auth.exception.EmailAlreadyExistsException;
import com.stockpro.auth.exception.ApiException;
import com.stockpro.auth.exception.InvalidCredentialsException;
import com.stockpro.auth.exception.AccountDeactivatedException;
import com.stockpro.auth.exception.InvalidTokenException;
import com.stockpro.auth.exception.UserNotFoundException;
import com.stockpro.auth.exception.PasswordMismatchException;
import com.stockpro.auth.repository.AuditLogRepository;
import com.stockpro.auth.repository.PasswordResetOtpRepository;
import com.stockpro.auth.repository.UserRepository;
import com.stockpro.auth.service.AuthService;
import com.stockpro.auth.service.PasswordResetMailService;
import com.stockpro.auth.util.JwtUtil;
import com.stockpro.auth.util.RedisTokenStore;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTokenStore redisTokenStore;
    private final UserMapper userMapper;
    private final PasswordResetMailService passwordResetMailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${app.password-reset.otp-length}")
    private int otpLength;

    @Value("${app.password-reset.otp-expiry-minutes}")
    private int otpExpiryMinutes;

    public AuthServiceImpl(
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            PasswordResetOtpRepository passwordResetOtpRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            RedisTokenStore redisTokenStore,
            UserMapper userMapper,
            PasswordResetMailService passwordResetMailService) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTokenStore = redisTokenStore;
        this.userMapper = userMapper;
        this.passwordResetMailService = passwordResetMailService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, boolean isAdminCreating) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User.Role role = resolveRegistrationRole(request, isAdminCreating);

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .isActive(true)
                .build();

        User saved = userRepository.save(user);
        passwordResetMailService.sendWelcome(saved.getEmail(), saved.getFullName());
        audit(null, "USER_REGISTERED", saved.getUserId(), saved.getEmail());
        
        return buildAuthResponse(saved);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.getIsActive()) throw new AccountDeactivatedException();

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String googleToken) {
        GoogleIdToken.Payload payload = verifyGoogleToken(googleToken);
        String email = payload.getEmail().toLowerCase();
        String name = (String) payload.get("name");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .fullName(name != null ? name : email)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                    .role(User.Role.WAREHOUSE_STAFF)
                    .isActive(true)
                    .build();
            User saved = userRepository.save(newUser);
            passwordResetMailService.sendWelcome(saved.getEmail(), saved.getFullName());
            audit(null, "GOOGLE_REGISTER", saved.getUserId(), email);
            return saved;
        });

        if (!user.getIsActive()) throw new AccountDeactivatedException();
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(String accessToken) {
        try {
            Long userId = jwtUtil.extractUserId(accessToken);
            String email = jwtUtil.extractEmail(accessToken);
            redisTokenStore.blacklistAccessToken(accessToken);
            redisTokenStore.deleteRefreshToken(userId);
            redisTokenStore.deleteSession(email);
            audit(userId, "LOGOUT", userId, email);
        } catch (Exception e) {
            log.warn("Logout failed");
        }
    }

    @Override
    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!jwtUtil.isTokenValid(token) || !jwtUtil.isRefreshToken(token)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        Long userId = jwtUtil.extractUserId(token);
        if (!redisTokenStore.isRefreshTokenValid(userId, token)) {
            throw new InvalidTokenException("Token revoked");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return TokenRefreshResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user))
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiryMs() / 1000)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshFullAuth(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        Long userId = jwtUtil.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return buildAuthResponse(user);
    }

    @Override
    public boolean validateToken(String token) {
        return jwtUtil.isTokenValid(token) && !redisTokenStore.isBlacklisted(token);
    }

    @Override
    public UserResponse getUserById(Long userId) {
        return userMapper.toUserResponse(userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId)));
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        return userMapper.toUserResponse(userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException(email)));
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userMapper.toUserResponseList(userRepository.findAll());
    }

    @Override
    public List<UserResponse> getUsersByRole(User.Role role) {
        return userMapper.toUserResponseList(userRepository.findAllByRole(role));
    }

    @Override
    public List<Long> getUserIdsByRole(String roleName) {
        try {
            User.Role role = User.Role.valueOf(roleName.toUpperCase());
            return userRepository.findAllByRole(role).stream()
                    .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                    .map(User::getUserId)
                    .collect(java.util.stream.Collectors.toList());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role requested: {}", roleName);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new PasswordMismatchException();
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        redisTokenStore.deleteRefreshToken(userId);
        redisTokenStore.deleteSession(user.getEmail());
        audit(userId, "PASSWORD_CHANGED", userId, user.getEmail());
    }

    @Override
    @Transactional
    public void sendForgotPasswordOtp(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ApiException("No active account found for that email", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ApiException("This account is inactive. Please contact an administrator.", HttpStatus.FORBIDDEN);
        }

        String otp = generateOtp();
        List<PasswordResetOtp> existingOtps =
                passwordResetOtpRepository.findAllByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(normalizedEmail);

        if (!existingOtps.isEmpty()) {
            existingOtps.forEach(existingOtp -> existingOtp.setUsed(true));
            passwordResetOtpRepository.saveAll(existingOtps);
        }

        PasswordResetOtp resetOtp = PasswordResetOtp.builder()
                .user(user)
                .email(normalizedEmail)
                .otpHash(passwordEncoder.encode(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .used(false)
                .build();

        passwordResetOtpRepository.save(resetOtp);
        passwordResetMailService.sendOtp(normalizedEmail, user.getFullName(), otp, otpExpiryMinutes);
        audit(user.getUserId(), "PASSWORD_RESET_OTP_SENT", user.getUserId(), normalizedEmail);
    }

    @Override
    @Transactional
    public void resetPasswordWithOtp(ResetPasswordWithOtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ApiException("Invalid email or OTP", HttpStatus.BAD_REQUEST));

        List<PasswordResetOtp> pendingOtps =
                passwordResetOtpRepository.findAllByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(normalizedEmail);

        if (pendingOtps.isEmpty()) {
            throw new ApiException("Invalid email or OTP", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean hasActiveOtp = pendingOtps.stream()
                .anyMatch(otp -> !otp.isUsed() && !otp.getExpiresAt().isBefore(now));

        if (!hasActiveOtp) {
            throw new ApiException("OTP has expired. Please request a new one.", HttpStatus.BAD_REQUEST);
        }

        pendingOtps.stream()
                .filter(otp -> !otp.isUsed() && !otp.getExpiresAt().isBefore(now))
                .filter(otp -> passwordEncoder.matches(request.getOtp().trim(), otp.getOtpHash()))
                .findFirst()
                .orElseThrow(() -> new ApiException("Invalid email or OTP", HttpStatus.BAD_REQUEST));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        pendingOtps.stream()
                .filter(otp -> !otp.isUsed())
                .forEach(otp -> otp.setUsed(true));
        passwordResetOtpRepository.saveAll(pendingOtps);

        redisTokenStore.deleteRefreshToken(user.getUserId());
        redisTokenStore.deleteSession(user.getEmail());
        passwordResetMailService.sendResetSuccess(normalizedEmail, user.getFullName());
        audit(user.getUserId(), "PASSWORD_RESET_COMPLETED", user.getUserId(), normalizedEmail);
    }

    @Override
    @Transactional
    public UserResponse adminUpdateUser(Long targetId, AdminUpdateUserRequest request, Long actorId) {
        User user = userRepository.findById(targetId)
                .orElseThrow(() -> new UserNotFoundException(targetId));

        User.Role currentRole = user.getRole();
        boolean currentActive = Boolean.TRUE.equals(user.getIsActive());

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getRole() != null) {
            validateManagedRole(request.getRole());
            if (currentRole == User.Role.ADMIN && request.getRole() != User.Role.ADMIN && currentActive) {
                ensureAnotherActiveAdminExists(user.getUserId());
            }
            user.setRole(request.getRole());
        }
        if (request.getIsActive() != null) user.setIsActive(request.getIsActive());
        if (currentRole == User.Role.ADMIN && currentActive && Boolean.FALSE.equals(user.getIsActive())) {
            ensureAnotherActiveAdminExists(user.getUserId());
        }

        User saved = userRepository.save(user);
        notifyAccountStatusChangeIfNeeded(saved, currentActive);
        audit(actorId, "ADMIN_UPDATED_USER", targetId, saved.getEmail());
        return userMapper.toUserResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId, Long actorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getRole() == User.Role.ADMIN && Boolean.TRUE.equals(user.getIsActive())) {
            ensureAnotherActiveAdminExists(user.getUserId());
        }
        user.setIsActive(false);
        User saved = userRepository.save(user);

        try {
            redisTokenStore.deleteRefreshToken(userId);
            redisTokenStore.deleteSession(user.getEmail());
        } catch (Exception exception) {
            log.warn("User {} was deactivated but session cleanup failed", userId, exception);
        }

        notifyAccountStatusChangeIfNeeded(saved, true);
        audit(actorId, "USER_DEACTIVATED", userId, user.getEmail());
    }

    private void notifyAccountStatusChangeIfNeeded(User user, boolean previousActive) {
        boolean currentActive = Boolean.TRUE.equals(user.getIsActive());
        if (previousActive == currentActive) {
            return;
        }
        if (currentActive) {
            passwordResetMailService.sendAccountReactivated(user.getEmail(), user.getFullName());
            return;
        }
        passwordResetMailService.sendAccountDeactivated(user.getEmail(), user.getFullName());
    }

    private User.Role resolveRegistrationRole(RegisterRequest request, boolean isAdminCreating) {
        if (!isAdminCreating) {
            return User.Role.WAREHOUSE_STAFF;
        }

        User.Role requestedRole = request.getRole() != null ? request.getRole() : User.Role.WAREHOUSE_STAFF;
        validateManagedRole(requestedRole);
        return requestedRole;
    }

    private void validateManagedRole(User.Role role) {
        if (role == User.Role.SYSTEM || role == User.Role.SUPPLIER) {
            throw new IllegalArgumentException("Unsupported role for employee management: " + role);
        }
    }

    private void ensureAnotherActiveAdminExists(Long excludedUserId) {
        long activeAdmins = userRepository.findAll().stream()
                .filter(candidate -> !candidate.getUserId().equals(excludedUserId))
                .filter(candidate -> candidate.getRole() == User.Role.ADMIN)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getIsActive()))
                .count();

        if (activeAdmins == 0) {
            throw new IllegalStateException("At least one active admin must remain in the system");
        }
    }

    private String generateOtp() {
        int effectiveLength = Math.max(4, otpLength);
        int floor = 1;
        for (int index = 1; index < effectiveLength; index++) {
            floor *= 10;
        }
        int bound = floor * 10;
        return String.valueOf(floor + secureRandom.nextInt(bound - floor));
    }

    private AuthResponse buildAuthResponse(User user) {
        String access = jwtUtil.generateAccessToken(user);
        String refresh = jwtUtil.generateRefreshToken(user);
        redisTokenStore.storeRefreshToken(user.getUserId(), refresh);
        redisTokenStore.storeSession(user.getEmail(), user.getUserId());
        user.setLastLoginAt(LocalDateTime.now());

        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiryMs() / 1000)
                .user(userMapper.toUserResponse(user))
                .build();
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            GoogleIdToken token = verifier.verify(idTokenString);
            if (token == null) throw new InvalidTokenException("Invalid Google token");
            return token.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidTokenException("Google verification failed");
        }
    }

    private void audit(Long actorId, String action, Long targetId, String details) {
        try {
            auditLogRepository.save(AuthAuditLog.builder()
                    .actorId(actorId).action(action).targetId(targetId).details(details).build());
        } catch (Exception exception) {
            log.debug("Skipping audit persistence for action {}", action, exception);
        }
    }
}
