package com.stockpro.auth.service.impl;

import com.stockpro.auth.exception.ApiException;
import com.stockpro.auth.service.PasswordResetMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetMailServiceImpl implements PasswordResetMailService {

    private static final String GENERIC_RECIPIENT_NAME = "there";
    private static final String COMMON_FONT_FAMILY = "Arial,sans-serif";
    private static final String COMMON_TEXT_COLOR = "#1f2937";
    private static final String COMMON_MUTED_TEXT_COLOR = "#475569";
    private static final String COMMON_HIGHLIGHT_COLOR = "#9a3412";
    private static final String COMMON_WARNING_TEXT_COLOR = "#7c2d12";
    private static final String COMMON_DARK_ACCENT = "#0f172a";
    private static final String SECURITY_TEAM_NAME = "StockPro Security Team";
    private static final String CONTENT_PADDING = "padding:32px;";
    private static final String WRAPPER_PADDING_STANDARD = "padding:32px 16px;";
    private static final String WRAPPER_PADDING_LARGE = "padding:36px 16px;";
    private static final String FOOTER_PADDING_WIDE = "0 32px 30px";
    private static final String FOOTER_PADDING_COMPACT = "0 32px 28px";
    private static final String DIVIDER_HTML = """
            <tr>
              <td style="padding:0 32px 32px;">
                <div style="height:1px;background:#e2e8f0;"></div>
              </td>
            </tr>
            """;
    private static final String STANDARD_CARD_STYLE =
            "max-width:620px;background:#ffffff;border-radius:24px;overflow:hidden;box-shadow:0 18px 50px rgba(15,23,42,0.12);";
    private static final String LARGE_CARD_STYLE =
            "max-width:640px;background:#ffffff;border-radius:28px;overflow:hidden;box-shadow:0 24px 60px rgba(15,23,42,0.14);";

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${app.mail.dev-log-otp-on-failure:false}")
    private boolean logOtpOnFailure;

    @Override
    public void sendWelcome(String recipientEmail, String recipientName) {
        sendIgnoringFailure(
                recipientEmail,
                "Welcome to StockPro",
                buildWelcomeHtml(recipientName),
                "Welcome email could not be sent to {} after account creation"
        );
    }

    @Override
    public void sendOtp(String recipientEmail, String recipientName, String otp, int expiryMinutes) {
        try {
            send(
                    recipientEmail,
                    "StockPro password reset OTP",
                    buildOtpHtml(recipientName, otp, expiryMinutes)
            );
        } catch (ApiException ex) {
            if (!logOtpOnFailure) {
                throw ex;
            }

            log.warn("SMTP unavailable. Dev fallback enabled for {}. OTP: {} (expires in {} minutes)",
                    recipientEmail, otp, expiryMinutes);
        }
    }

    @Override
    public void sendResetSuccess(String recipientEmail, String recipientName) {
        sendIgnoringFailure(
                recipientEmail,
                "StockPro password reset successful",
                buildResetSuccessHtml(recipientName),
                "Password reset completed for {} but confirmation email could not be sent"
        );
    }

    @Override
    public void sendAccountDeactivated(String recipientEmail, String recipientName) {
        sendIgnoringFailure(
                recipientEmail,
                "Your StockPro account has been deactivated",
                buildAccountDeactivatedHtml(recipientName),
                "Account deactivation completed for {} but notification email could not be sent"
        );
    }

    @Override
    public void sendAccountReactivated(String recipientEmail, String recipientName) {
        sendIgnoringFailure(
                recipientEmail,
                "Your StockPro account has been reactivated",
                buildAccountReactivatedHtml(recipientName),
                "Account reactivation completed for {} but notification email could not be sent"
        );
    }

    private void sendIgnoringFailure(String recipientEmail, String subject, String htmlBody, String warningMessage) {
        try {
            send(recipientEmail, subject, htmlBody);
        } catch (ApiException ex) {
            log.warn(warningMessage, recipientEmail, ex);
        }
    }

    private void send(String recipientEmail, String subject, String htmlBody) {
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Mail sent to {} with subject '{}'", recipientEmail, subject);
        } catch (MailAuthenticationException ex) {
            log.error("SMTP authentication failed for configured account '{}'. Check MAIL_USER and MAIL_PASS. "
                    + "For Gmail, use a valid App Password with 2-Step Verification enabled.", smtpUsername, ex);
            throw new ApiException("Email service authentication failed. Please contact support.", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception ex) {
            log.error("Failed to send mail to {}", recipientEmail, ex);
            throw new ApiException("Unable to send email right now. Please try again later.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private String getSafeRecipientName(String recipientName) {
        return HtmlUtils.htmlEscape(
                recipientName == null || recipientName.isBlank() ? GENERIC_RECIPIENT_NAME : recipientName
        );
    }

    private String buildOtpHtml(String recipientName, String otp, int expiryMinutes) {
        String safeName = getSafeRecipientName(recipientName);
        String safeOtp = HtmlUtils.htmlEscape(otp);
        String bodyContent = contentSection(
                safeName,
                """
                <p style="margin:0 0 24px;font-size:16px;line-height:1.7;color:#4b5563;">
                  We received a request to reset your StockPro password. Enter this one-time password in the app to continue.
                </p>
                %s
                %s
                """.formatted(
                        centeredPanel(
                                "margin:0 0 24px;padding:24px;border-radius:20px;background:#fff7ed;border:1px solid #fed7aa;",
                                "Your OTP Code",
                                COMMON_HIGHLIGHT_COLOR,
                                """
                                <div style="font-size:34px;letter-spacing:0.3em;font-weight:800;color:#111827;">%s</div>
                                <div style="margin-top:10px;font-size:14px;color:%s;">Valid for %d minutes</div>
                                """.formatted(safeOtp, COMMON_WARNING_TEXT_COLOR, expiryMinutes)
                        ),
                        messagePanel(
                                "padding:18px 20px;border-radius:18px;background:#f8fafc;border:1px solid #e5e7eb;",
                                "Security tips",
                                "#111827",
                                "#4b5563",
                                "If you did not request this reset, you can safely ignore this email. Never share this OTP with anyone."
                        )
                )
        );
        return buildEmailPage(
                new EmailPageLayout(
                        "StockPro Password Reset OTP",
                        "#f4efe7",
                        "linear-gradient(135deg,#f5efe6 0%,#eef4f8 100%)",
                        WRAPPER_PADDING_STANDARD,
                        STANDARD_CARD_STYLE
                ),
                headerSection(
                        COMMON_TEXT_COLOR,
                        "padding:28px 32px;text-align:center;",
                        logoBadge("#f59e0b", "S", "22px", "48px", "16px", ""),
                        "",
                        "Reset your StockPro password",
                        "#d1d5db",
                        "Use the secure verification code below to continue."
                ),
                bodyContent,
                simpleFooter(SECURITY_TEAM_NAME, "Helping you keep your inventory operations safe.", FOOTER_PADDING_COMPACT)
        );
    }

    private String buildWelcomeHtml(String recipientName) {
        String safeName = getSafeRecipientName(recipientName);
        String bodyContent = contentSection(
                safeName,
                """
                <p style="margin:0 0 24px;font-size:16px;line-height:1.8;color:%s;">
                  Welcome to StockPro. Your account has been created successfully, and your access is now active.
                </p>
                %s
                %s
                """.formatted(
                        COMMON_MUTED_TEXT_COLOR,
                        messagePanel(
                                "padding:22px;border-radius:22px;background:linear-gradient(135deg,#eff6ff 0%,#f0fdf4 100%);border:1px solid #bfdbfe;margin-bottom:24px;",
                                "What you can do next",
                                "#1d4ed8",
                                "#1e3a8a",
                                "Sign in to StockPro and begin using the features available for your role."
                        ),
                        messagePanel(
                                "padding:22px;border-radius:22px;background:#fff7ed;border:1px solid #fed7aa;",
                                "Need help?",
                                COMMON_HIGHLIGHT_COLOR,
                                COMMON_WARNING_TEXT_COLOR,
                                "If you were not expecting this account or need help accessing it, please contact your StockPro administrator."
                        )
                )
        );
        return buildEmailPage(
                new EmailPageLayout(
                        "Welcome to StockPro",
                        "#eef4f8",
                        "linear-gradient(135deg,#eef8f1 0%,#eef4fb 55%,#fff6e8 100%)",
                        WRAPPER_PADDING_LARGE,
                        LARGE_CARD_STYLE
                ),
                headerSection(
                        "linear-gradient(135deg,#0f172a 0%,#1f3a5f 100%)",
                        "padding:32px 32px 28px;text-align:center;",
                        logoBadge("linear-gradient(135deg,#f59e0b 0%,#f97316 100%)", "S", "26px", "56px", "18px",
                                "box-shadow:0 10px 22px rgba(249,115,22,0.28);"),
                        headerEyebrow("Welcome", "#fde68a"),
                        "Your StockPro account is ready",
                        "#dbe7f3",
                        "You can now use StockPro to manage inventory, movements, and operations."
                ),
                bodyContent + DIVIDER_HTML,
                simpleFooter("StockPro Team", "This is an automated welcome email for your new account.", FOOTER_PADDING_WIDE)
        );
    }

    private String buildResetSuccessHtml(String recipientName) {
        String safeName = getSafeRecipientName(recipientName);
        String bodyContent = contentSection(
                safeName,
                """
                <p style="margin:0 0 24px;font-size:16px;line-height:1.8;color:%s;">
                  Your account is ready to use with the new password. For your safety, we are letting you know immediately whenever a credential change happens.
                </p>
                %s
                %s
                %s
                """.formatted(
                        COMMON_MUTED_TEXT_COLOR,
                        messagePanel(
                                "padding:22px;border-radius:22px;background:linear-gradient(135deg,#f0fdf4 0%,#ecfeff 100%);border:1px solid #bbf7d0;margin-bottom:24px;",
                                "What this means",
                                "#15803d",
                                "#065f46",
                                "You can now sign in to StockPro using your newly created password."
                        ),
                        messagePanel(
                                "padding:22px;border-radius:22px;background:#f8fafc;border:1px solid #e2e8f0;margin-bottom:24px;",
                                "Recommended next steps",
                                COMMON_DARK_ACCENT,
                                COMMON_MUTED_TEXT_COLOR,
                                "Use a strong password you do not reuse elsewhere. If you stay signed in on multiple devices, signing out and back in can help confirm everything is updated properly."
                        ),
                        messagePanel(
                                "padding:22px;border-radius:22px;background:#fff7ed;border:1px solid #fed7aa;",
                                "Did not make this change?",
                                COMMON_HIGHLIGHT_COLOR,
                                COMMON_WARNING_TEXT_COLOR,
                                "Contact your administrator or support team immediately and reset the account again if you believe this activity was not authorized."
                        )
                )
        );
        return buildEmailPage(
                new EmailPageLayout(
                        "StockPro Password Reset Successful",
                        "#edf4f7",
                        "linear-gradient(135deg,#e8f6ee 0%,#edf4fb 52%,#fff6e8 100%)",
                        WRAPPER_PADDING_LARGE,
                        LARGE_CARD_STYLE
                ),
                headerSection(
                        "linear-gradient(135deg,#0f172a 0%,#1f3a5f 100%)",
                        "padding:32px 32px 28px;text-align:center;",
                        logoBadge("linear-gradient(135deg,#22c55e 0%,#16a34a 100%)", "✓", "26px", "56px", "18px",
                                "box-shadow:0 10px 22px rgba(34,197,94,0.28);"),
                        headerEyebrow("Security Confirmation", "#86efac"),
                        "Your password has been changed",
                        "#dbe7f3",
                        "This email confirms that your StockPro account password was reset successfully."
                ),
                bodyContent + DIVIDER_HTML,
                simpleFooter(
                        SECURITY_TEAM_NAME,
                        "This is an automated account safety notification for your workspace. If you need help, please reach out to your administrator.",
                        FOOTER_PADDING_WIDE
                ) + centeredFinePrint("StockPro Inventory Management")
        );
    }

    private String buildAccountDeactivatedHtml(String recipientName) {
        String safeName = getSafeRecipientName(recipientName);
        String bodyContent = contentSection(
                safeName,
                """
                <p style="margin:0 0 22px;font-size:16px;line-height:1.8;color:%s;">
                  An administrator has deactivated your StockPro access. This means your account is temporarily unavailable until it is restored by your organization.
                </p>
                %s
                %s
                """.formatted(
                        COMMON_MUTED_TEXT_COLOR,
                        messagePanel(
                                "padding:20px 22px;border-radius:20px;background:#fef2f2;border:1px solid #fecaca;margin-bottom:22px;",
                                "What happens now",
                                "#991b1b",
                                "#7f1d1d",
                                "You will not be able to log in or continue inventory operations in StockPro while the account remains inactive."
                        ),
                        messagePanel(
                                "padding:20px 22px;border-radius:20px;background:#f8fafc;border:1px solid #e2e8f0;",
                                "Need help?",
                                COMMON_DARK_ACCENT,
                                COMMON_MUTED_TEXT_COLOR,
                                "Please contact your StockPro administrator if you believe this change was made in error or if you need your access restored."
                        )
                )
        );
        return buildEmailPage(
                new EmailPageLayout(
                        "StockPro Account Deactivated",
                        "#f8f1f1",
                        "linear-gradient(135deg,#fff1f2 0%,#f8fafc 100%)",
                        WRAPPER_PADDING_STANDARD,
                        STANDARD_CARD_STYLE
                ),
                headerSection(
                        "#7f1d1d",
                        "padding:30px 32px;text-align:center;",
                        logoBadge("#ef4444", "!", "24px", "52px", "18px", ""),
                        "",
                        "Your StockPro account has been deactivated",
                        "#fecaca",
                        "You currently cannot sign in to StockPro."
                ),
                bodyContent,
                simpleFooter(SECURITY_TEAM_NAME, "This is an automated account access notification.", FOOTER_PADDING_WIDE)
        );
    }

    private String buildAccountReactivatedHtml(String recipientName) {
        String safeName = getSafeRecipientName(recipientName);
        String bodyContent = contentSection(
                safeName,
                """
                <p style="margin:0 0 22px;font-size:16px;line-height:1.8;color:%s;">
                  An administrator has reactivated your StockPro account. You can now sign in again and continue using the platform.
                </p>
                %s
                %s
                """.formatted(
                        COMMON_MUTED_TEXT_COLOR,
                        messagePanel(
                                "padding:20px 22px;border-radius:20px;background:#f0fdf4;border:1px solid #bbf7d0;margin-bottom:22px;",
                                "You can sign in now",
                                "#166534",
                                "#166534",
                                "Use your existing credentials to access StockPro. If you have trouble signing in, contact your administrator."
                        ),
                        messagePanel(
                                "padding:20px 22px;border-radius:20px;background:#f8fafc;border:1px solid #e2e8f0;",
                                "Security reminder",
                                COMMON_DARK_ACCENT,
                                COMMON_MUTED_TEXT_COLOR,
                                "If you were not expecting this change, please let your administrator know right away."
                        )
                )
        );
        return buildEmailPage(
                new EmailPageLayout(
                        "StockPro Account Reactivated",
                        "#eef7f1",
                        "linear-gradient(135deg,#ecfdf5 0%,#eff6ff 100%)",
                        WRAPPER_PADDING_STANDARD,
                        STANDARD_CARD_STYLE
                ),
                headerSection(
                        "#14532d",
                        "padding:30px 32px;text-align:center;",
                        logoBadge("#22c55e", "✓", "24px", "52px", "18px", ""),
                        "",
                        "Your StockPro account has been reactivated",
                        "#bbf7d0",
                        "Your access to StockPro has been restored."
                ),
                bodyContent,
                simpleFooter(SECURITY_TEAM_NAME, "This is an automated account access notification.", FOOTER_PADDING_WIDE)
        );
    }

    private String buildEmailPage(EmailPageLayout layout, String headerHtml, String contentHtml, String footerHtml) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:%s;font-family:%s;color:%s;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:%s;%s">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="%s">
                          %s
                          %s
                          %s
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                layout.title(),
                layout.bodyBackground(),
                COMMON_FONT_FAMILY,
                COMMON_TEXT_COLOR,
                layout.tableBackground(),
                layout.tablePadding(),
                layout.cardStyle(),
                headerHtml,
                contentHtml,
                footerHtml
        );
    }

    private String headerSection(String background, String padding, String badgeHtml, String eyebrowHtml, String title,
                                 String subtitleColor, String subtitle) {
        return """
                <tr>
                  <td style="background:%s;%s">
                    %s
                    %s
                    <h1 style="margin:16px 0 8px;color:#ffffff;font-size:30px;line-height:1.2;">%s</h1>
                    <p style="margin:0;color:%s;font-size:15px;line-height:1.7;">%s</p>
                  </td>
                </tr>
                """.formatted(background, padding, badgeHtml, eyebrowHtml, title, subtitleColor, subtitle);
    }

    private String logoBadge(String background, String symbol, String fontSize, String size, String radius, String extraStyle) {
        return """
                <div style="display:inline-block;background:%s;color:#ffffff;font-size:%s;line-height:%s;width:%s;height:%s;border-radius:%s;font-weight:700;%s">%s</div>
                """.formatted(background, fontSize, size, size, size, radius, extraStyle, symbol);
    }

    private String headerEyebrow(String text, String color) {
        return """
                <div style="margin:18px 0 8px;font-size:12px;letter-spacing:0.16em;text-transform:uppercase;color:%s;font-weight:700;">%s</div>
                """.formatted(color, text);
    }

    private String contentSection(String safeName, String innerHtml) {
        return """
                <tr>
                  <td style="%s">
                    <p style="margin:0 0 14px;font-size:16px;line-height:1.7;">Hello %s,</p>
                    %s
                  </td>
                </tr>
                """.formatted(CONTENT_PADDING, safeName, innerHtml);
    }

    private String messagePanel(String panelStyle, String title, String titleColor, String textColor, String body) {
        return """
                <div style="%s">
                  <div style="font-size:14px;font-weight:700;color:%s;margin-bottom:8px;">%s</div>
                  <div style="font-size:14px;line-height:1.8;color:%s;">
                    %s
                  </div>
                </div>
                """.formatted(panelStyle, titleColor, title, textColor, body);
    }

    private String centeredPanel(String panelStyle, String title, String titleColor, String bodyHtml) {
        return """
                <div style="%stext-align:center;">
                  <div style="margin-bottom:8px;font-size:13px;letter-spacing:0.08em;text-transform:uppercase;color:%s;font-weight:700;">%s</div>
                  %s
                </div>
                """.formatted(panelStyle, titleColor, title, bodyHtml);
    }

    private String simpleFooter(String teamName, String footerText, String padding) {
        return """
                <tr>
                  <td style="padding:%s;color:#64748b;font-size:13px;line-height:1.8;">
                    <strong style="color:%s;">%s</strong><br>
                    %s
                  </td>
                </tr>
                """.formatted(padding, COMMON_DARK_ACCENT, teamName, footerText);
    }

    private String centeredFinePrint(String text) {
        return """
                <tr>
                  <td style="padding:0 32px 32px;">
                    <div style="font-size:12px;line-height:1.7;color:#94a3b8;text-align:center;">
                      %s
                    </div>
                  </td>
                </tr>
                """.formatted(text);
    }

    private record EmailPageLayout(
            String title,
            String bodyBackground,
            String tableBackground,
            String tablePadding,
            String cardStyle
    ) {
    }
}
