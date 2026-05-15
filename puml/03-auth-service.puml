@startuml
title Auth Service

interface AuthService {
  +register()
  +login()
  +loginWithGoogle()
  +logout()
  +refreshToken()
  +validateToken()
  +getUserById()
  +updateProfile()
  +changePassword()
  +sendForgotPasswordOtp()
  +resetPasswordWithOtp()
  +adminUpdateUser()
  +deactivateUser()
}

class AuthController {
  +register()
  +login()
  +googleLogin()
  +logout()
  +refreshToken()
  +getCurrentUser()
}

class AuthServiceImpl {
  +register()
  +login()
  +loginWithGoogle()
  +logout()
  +refreshToken()
  +validateToken()
  +getUserById()
  +updateProfile()
  +changePassword()
  +sendForgotPasswordOtp()
  +resetPasswordWithOtp()
  +adminUpdateUser()
  +deactivateUser()
}

class UserRepository {
  +findByEmail()
  +existsByEmail()
  +findAllByRole()
  +existsByRole()
}

class AuditLogRepository {
  +save()
}

class PasswordResetOtpRepository {
  +findTopByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc()
  +save()
}

class JwtUtil {
  +generateAccessToken()
  +generateRefreshToken()
  +extractEmail()
  +extractUserId()
  +extractRole()
  +isTokenValid()
  +isRefreshToken()
  +getAccessTokenExpiryMs()
  +getRemainingValidity()
}

class RedisTokenStore {
  +storeRefreshToken()
  +getRefreshToken()
  +deleteRefreshToken()
  +isRefreshTokenValid()
  +blacklistAccessToken()
  +isBlacklisted()
  +storeSession()
  +deleteSession()
}

interface PasswordResetMailService {
  +sendOtp()
  +sendResetSuccess()
}

class User {
  +onCreate()
}

class AuthAuditLog {
  +builder()
}

class PasswordResetOtp {
  +builder()
}

AuthController --> AuthService
AuthService <|.. AuthServiceImpl
AuthServiceImpl --> UserRepository
AuthServiceImpl --> AuditLogRepository
AuthServiceImpl --> PasswordResetOtpRepository
AuthServiceImpl --> JwtUtil
AuthServiceImpl --> RedisTokenStore
AuthServiceImpl --> PasswordResetMailService
UserRepository --> User
AuditLogRepository --> AuthAuditLog
PasswordResetOtpRepository --> PasswordResetOtp
@enduml
