package com.stockpro.auth.controller;

import com.stockpro.auth.dto.request.AuthRequestDTOs.*;
import com.stockpro.auth.dto.*;
import com.stockpro.auth.service.AuthService;
import com.stockpro.auth.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication, profile, token, and user administration APIs")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<AuthResponse>builder()
                        .success(true)
                        .message("User registered successfully")
                        .data(authService.register(request, false))
                        .build());
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive access and refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Login successful")
                .data(authService.login(request))
                .build());
    }

    @PostMapping("/google-login")
    @Operation(summary = "Log in with a Google identity token")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Google login successful")
                .data(authService.loginWithGoogle(request.token()))
                .build());
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out and invalidate the current session")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String token) {
        authService.logout(extractBearerToken(token));
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Logged out successfully")
                .build());
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh an expired access token")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.<TokenRefreshResponse>builder()
                .success(true)
                .message("Token refreshed successfully")
                .data(authService.refreshToken(request))
                .build());
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate a JWT token")
    public ResponseEntity<Boolean> validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        return ResponseEntity.ok(authService.validateToken(request.getToken()));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(extractBearerToken(token));
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .data(authService.getUserById(userId))
                .build());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update the current user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Profile updated")
                .data(authService.updateProfile(userId, request))
                .build());
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change the current user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Password changed")
                .build());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send a password reset OTP")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.sendForgotPasswordOtp(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("OTP sent to your email")
                .build());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordWithOtpRequest request) {
        authService.resetPasswordWithOtp(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Password reset successful")
                .build());
    }

    // Admin endpoints
    @GetMapping("/users")
    @Operation(summary = "Get all users [ADMIN only]")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.<List<UserResponse>>builder()
                .success(true)
                .data(authService.getAllUsers())
                .build());
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get a user by ID [ADMIN only]")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .data(authService.getUserById(userId))
                .build());
    }

    @GetMapping("/api/users/ids-by-role")
    @Operation(summary = "Get user IDs by role")
    public ResponseEntity<ApiResponse<List<Long>>> getUserIdsByRole(@RequestParam String role) {
        return ResponseEntity.ok(ApiResponse.<List<Long>>builder()
                .success(true)
                .data(authService.getUserIdsByRole(role))
                .build());
    }

    @PutMapping("/admin/users/{userId}")
    @Operation(summary = "Update a user as admin [ADMIN only]")
    public ResponseEntity<ApiResponse<UserResponse>> adminUpdateUser(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long adminId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User updated by admin")
                .data(authService.adminUpdateUser(userId, request, adminId))
                .build());
    }

    @DeleteMapping("/admin/users/{userId}")
    @Operation(summary = "Deactivate a user as admin [ADMIN only]")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long adminId) {
        authService.deactivateUser(userId, adminId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("User deactivated")
                .build());
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must use Bearer token");
        }
        return authorizationHeader.substring(7);
    }

    public record GoogleLoginRequest(String token) {}
}
