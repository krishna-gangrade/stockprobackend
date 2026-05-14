package com.stockpro.auth.dto.request;

import com.stockpro.auth.entity.User;
import com.stockpro.auth.validation.NumericOtp;
import com.stockpro.auth.validation.StrongPassword;
import com.stockpro.auth.validation.ValidEmailAddress;
import com.stockpro.auth.validation.ValidFullName;
import jakarta.validation.constraints.*;
import lombok.Data;

// ─── Register ──────────────────────────────────────────────────────────────────
public final class AuthRequestDTOs {

    private static final String EMAIL_MESSAGE =
            "Email must be valid, cannot contain consecutive dots, and must have an alphabetic domain like gmail.com";
    private static final String NEW_PASSWORD_MESSAGE =
            "New password must contain uppercase, lowercase, digit, and special character";

    private AuthRequestDTOs() {
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100)
        @ValidFullName
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @ValidEmailAddress(message = EMAIL_MESSAGE)
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @StrongPassword
        private String password;

        @Size(max = 20)
        private String phone;

        // Admin-only: role assignment; defaults to STAFF if null
        private User.Role role;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @ValidEmailAddress(message = EMAIL_MESSAGE)
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Data
    public static class ValidateTokenRequest {
        @NotBlank(message = "Token is required")
        private String token;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8)
        @StrongPassword(message = NEW_PASSWORD_MESSAGE)
        private String newPassword;
    }

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @ValidEmailAddress(message = EMAIL_MESSAGE)
        private String email;
    }

    @Data
    public static class ResetPasswordWithOtpRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @ValidEmailAddress(message = EMAIL_MESSAGE)
        private String email;

        @NotBlank(message = "OTP is required")
        @NumericOtp
        private String otp;

        @NotBlank(message = "New password is required")
        @Size(min = 8)
        @StrongPassword(message = NEW_PASSWORD_MESSAGE)
        private String newPassword;
    }

    @Data
    public static class UpdateProfileRequest {
        @Size(min = 2, max = 100)
        @ValidFullName
        private String fullName;

        @Size(max = 20)
        private String phone;
    }

    @Data
    public static class AdminUpdateUserRequest {
        @Size(min = 2, max = 100)
        @ValidFullName
        private String fullName;

        @Size(max = 20)
        private String phone;

        private User.Role role;

        private Boolean isActive;
    }
}
