package com.stockpro.auth.service;

import static com.stockpro.auth.dto.request.AuthRequestDTOs.*;
import com.stockpro.auth.dto.AuthResponse;
import com.stockpro.auth.dto.UserResponse;
import com.stockpro.auth.dto.TokenRefreshResponse;
import com.stockpro.auth.entity.User;

import java.util.List;

public interface AuthService {
    AuthResponse register(RegisterRequest request, boolean isAdminCreating);
    
    AuthResponse login(LoginRequest request);
    
    AuthResponse loginWithGoogle(String googleToken);
    
    void logout(String accessToken);
    
    TokenRefreshResponse refreshToken(RefreshTokenRequest request);
    
    AuthResponse refreshFullAuth(String refreshToken);
    
    boolean validateToken(String token);
    
    // User management
    UserResponse getUserById(Long userId);
    
    UserResponse getUserByEmail(String email);
    
    List<UserResponse> getAllUsers();
    
    List<UserResponse> getUsersByRole(User.Role role);
    
    List<Long> getUserIdsByRole(String roleName);
    
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    
    void changePassword(Long userId, ChangePasswordRequest request);

    void sendForgotPasswordOtp(ForgotPasswordRequest request);

    void resetPasswordWithOtp(ResetPasswordWithOtpRequest request);
    
    UserResponse adminUpdateUser(Long targetId, AdminUpdateUserRequest request, Long actorId);
    
    void deactivateUser(Long userId, Long actorId);
}
