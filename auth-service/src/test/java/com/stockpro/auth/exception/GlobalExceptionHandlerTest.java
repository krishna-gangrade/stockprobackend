package com.stockpro.auth.exception;

import com.stockpro.auth.dto.ApiResponse;
import com.stockpro.auth.dto.request.AuthRequestDTOs.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler unit tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesEmailAlreadyExists() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleEmailExists(new EmailAlreadyExistsException("jane@stockpro.com"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).contains("jane@stockpro.com");
    }

    @Test
    void handlesInvalidCredentials() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidCredentials(new InvalidCredentialsException());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handlesUserNotFound() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUserNotFound(new UserNotFoundException(7L));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handlesAccountDeactivated() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleDeactivated(new AccountDeactivatedException());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handlesPasswordMismatch() {
        ResponseEntity<ApiResponse<Void>> response = handler.handlePasswordMismatch(new PasswordMismatchException());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handlesInvalidToken() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidToken(new InvalidTokenException("bad token"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handlesApiException() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleApiException(
                new ApiException("mail down", HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getMessage()).isEqualTo("mail down");
    }

    @Test
    void handlesIllegalState() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalState(
                new IllegalStateException("At least one active admin must remain"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handlesIllegalArgument() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Unsupported role"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handlesGenericException() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(new RuntimeException("boom"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
    }

    @Test
    void handlesValidationErrorsWithJoinedMessages() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new RegisterRequest(), "registerRequest");
        bindingResult.addError(new FieldError("registerRequest", "email", "", false, null, null, "Email is required"));
        bindingResult.addError(new FieldError("registerRequest", "password", "", false, null, null, "Password is required"));

        Method method = DummyController.class.getDeclaredMethod("register", RegisterRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Email is required, Password is required");
    }

    @Test
    void handlesValidationErrorsWithoutMessages() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new RegisterRequest(), "registerRequest");
        bindingResult.addError(new FieldError("registerRequest", "email", "", false, null, null, ""));

        Method method = DummyController.class.getDeclaredMethod("register", RegisterRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(exception);

        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
    }

    private static class DummyController {
        @SuppressWarnings("unused")
        void register(RegisterRequest request) {
            // No implementation needed; this method only supplies reflection metadata for validation tests.
        }
    }
}
