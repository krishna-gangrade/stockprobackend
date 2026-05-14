package com.stockpro.auth.service;

import com.stockpro.auth.exception.ApiException;
import com.stockpro.auth.service.impl.PasswordResetMailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetMailServiceImpl unit tests")
class PasswordResetMailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private PasswordResetMailServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetMailServiceImpl(mailSender);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@stockpro.test");
        ReflectionTestUtils.setField(service, "logOtpOnFailure", false);
    }

    @Test
    void sendOtpBuildsAndSendsHtmlMail() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        service.sendOtp("jane@stockpro.com", "Jane <Admin>", "123456", 10);

        verify(mailSender).send(message);
        assertThat(message.getAllRecipients()[0]).hasToString("jane@stockpro.com");
        assertThat(message.getSubject()).isEqualTo("StockPro password reset OTP");
        String html = (String) message.getContent();
        assertThat(html)
                .contains("123456")
                .contains("Jane &lt;Admin&gt;")
                .contains("Valid for 10 minutes");
    }

    @Test
    void sendWelcomeBuildsAndSendsHtmlMail() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        service.sendWelcome("jane@stockpro.com", "Jane <Admin>");

        verify(mailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("Welcome to StockPro");
        String html = (String) message.getContent();
        assertThat(html)
                .contains("Jane &lt;Admin&gt;")
                .contains("Your StockPro account is ready")
                .contains("Welcome to StockPro");
    }

    @Test
    void sendResetSuccessBuildsAndSendsHtmlMail() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        service.sendResetSuccess("jane@stockpro.com", "");

        verify(mailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("StockPro password reset successful");
        String html = (String) message.getContent();
        assertThat(html)
                .contains("Hello there")
                .contains("Your password has been changed");
    }

    @Test
    void sendOtpRethrowsMailFailureWhenDevFallbackIsDisabled() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("smtp down"));

        assertThatThrownBy(() -> service.sendOtp("jane@stockpro.com", "Jane", "123456", 10))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unable to send email");
    }

    @Test
    void sendOtpSuppressesFailureWhenDevFallbackIsEnabled() {
        ReflectionTestUtils.setField(service, "logOtpOnFailure", true);
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("smtp down"));

        assertThatCode(() -> service.sendOtp("jane@stockpro.com", "Jane", "654321", 10))
                .doesNotThrowAnyException();
    }

    @Test
    void sendWelcomeSuppressesMailFailure() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("smtp down"));

        assertThatCode(() -> service.sendWelcome("jane@stockpro.com", "Jane"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendResetSuccessSuppressesMailFailure() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("smtp down"));

        assertThatCode(() -> service.sendResetSuccess("jane@stockpro.com", "Jane"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendAccountDeactivatedBuildsAndSendsHtmlMail() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        service.sendAccountDeactivated("jane@stockpro.com", "Jane <Admin>");

        verify(mailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("Your StockPro account has been deactivated");
        String html = (String) message.getContent();
        assertThat(html)
                .contains("Jane &lt;Admin&gt;")
                .contains("has been deactivated")
                .contains("cannot sign in");
    }

    @Test
    void sendAccountReactivatedBuildsAndSendsHtmlMail() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        service.sendAccountReactivated("jane@stockpro.com", "");

        verify(mailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("Your StockPro account has been reactivated");
        String html = (String) message.getContent();
        assertThat(html)
                .contains("Hello there")
                .contains("has been reactivated")
                .contains("sign in now");
    }

    @Test
    void sendAccountDeactivatedSuppressesMailFailure() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("smtp down"));

        assertThatCode(() -> service.sendAccountDeactivated("jane@stockpro.com", "Jane"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendAccountReactivatedSuppressesMailFailure() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("smtp down"));

        assertThatCode(() -> service.sendAccountReactivated("jane@stockpro.com", "Jane"))
                .doesNotThrowAnyException();
    }
}
