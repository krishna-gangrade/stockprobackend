package com.stockpro.auth.service;

public interface PasswordResetMailService {
    void sendWelcome(String recipientEmail, String recipientName);

    void sendOtp(String recipientEmail, String recipientName, String otp, int expiryMinutes);

    void sendResetSuccess(String recipientEmail, String recipientName);

    void sendAccountDeactivated(String recipientEmail, String recipientName);

    void sendAccountReactivated(String recipientEmail, String recipientName);
}
