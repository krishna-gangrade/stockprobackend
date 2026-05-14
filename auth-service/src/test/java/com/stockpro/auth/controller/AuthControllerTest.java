package com.stockpro.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import static com.stockpro.auth.dto.request.AuthRequestDTOs.*;
import com.stockpro.auth.dto.*;
import com.stockpro.auth.entity.User;
import com.stockpro.auth.service.AuthService;
import com.stockpro.auth.util.JwtUtil;
import com.stockpro.auth.util.RedisTokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController MockMvc Tests")
class AuthControllerTest {

    private static final String ADMIN_ROLE = "ADMIN";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AuthService authService;
    @MockBean
    JwtUtil jwtUtil;
    @MockBean
    RedisTokenStore redisTokenStore;
    @MockBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String BASE_URL = "/auth";

    // ── /auth/register ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/register returns 201 for valid request")
    void register_validRequest_returns201() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Jane Doe");
        req.setEmail("jane@stockpro.com");
        req.setPassword("Secret@123");

        UserResponse userResp = UserResponse.builder().userId(1L).fullName("Jane Doe").email("jane@stockpro.com")
                .role(User.Role.WAREHOUSE_STAFF).isActive(true).build();
        AuthResponse resp = AuthResponse.builder()
                .accessToken("token")
                .refreshToken("refresh")
                .tokenType("Bearer")
                .expiresIn(28800L)
                .user(userResp)
                .build();

        when(authService.register(any(RegisterRequest.class), eq(false))).thenReturn(resp);

        mockMvc.perform(post(BASE_URL + "/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)).with(csrf())).andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("jane@stockpro.com"));
    }

    @Test
    @DisplayName("POST /auth/register returns 400 for blank email")
    void register_blankEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Jane");
        req.setEmail(""); // invalid
        req.setPassword("Secret@123");

        mockMvc.perform(post(BASE_URL + "/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)).with(csrf())).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register returns 400 for full name with digits")
    void register_nameWithDigits_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Jane123");
        req.setEmail("jane@stockpro.com");
        req.setPassword("Secret@123");

        mockMvc.perform(post(BASE_URL + "/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Full name can contain letters and spaces only")));
    }

    @Test
    @DisplayName("POST /auth/register returns 400 for email with consecutive dots")
    void register_emailWithConsecutiveDots_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Jane Doe");
        req.setEmail("jane..doe@stockpro.com");
        req.setPassword("Secret@123");

        mockMvc.perform(post(BASE_URL + "/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot contain consecutive dots")));
    }

    @Test
    @DisplayName("POST /auth/register returns 400 for email domain containing digits")
    void register_emailDomainWithDigits_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Jane Doe");
        req.setEmail("jane@gma56il.com");
        req.setPassword("Secret@123");

        mockMvc.perform(post(BASE_URL + "/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("alphabetic domain like gmail.com")));
    }

    @Test
    @DisplayName("POST /auth/register returns 400 for weak password")
    void register_weakPassword_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Jane Doe");
        req.setEmail("jane@stockpro.com");
        req.setPassword("password");

        mockMvc.perform(post(BASE_URL + "/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Password must contain uppercase")));
    }

    // ── /auth/login ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/login returns 200 with tokens on success")
    void login_valid_returns200() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("jane@stockpro.com");
        req.setPassword("Secret@123");

        AuthResponse auth = AuthResponse.builder().accessToken("access-token").refreshToken("refresh-token")
                .tokenType("Bearer").expiresIn(28800L).build();

        when(authService.login(any())).thenReturn(auth);

        mockMvc.perform(post(BASE_URL + "/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)).with(csrf())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    // ── /auth/refresh-token ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/refresh-token returns 200 with new access token")
    void refresh_valid_returns200() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("valid-refresh-token");

        TokenRefreshResponse resp = TokenRefreshResponse.builder().accessToken("new-access-token").tokenType("Bearer")
                .expiresIn(28800L).build();

        when(authService.refreshToken(any())).thenReturn(resp);

        mockMvc.perform(post(BASE_URL + "/refresh-token").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)).with(csrf())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }

    // ── Admin-only endpoints ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /auth/users returns 200 for ADMIN role")
    @WithMockUser(roles = ADMIN_ROLE)
    void getUsers_admin_returns200() throws Exception {
        when(authService.getAllUsers()).thenReturn(java.util.List.of());

        mockMvc.perform(get(BASE_URL + "/users")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /auth/logout strips Bearer prefix and logs out")
    void logout_validHeader_returns200() throws Exception {
        mockMvc.perform(post(BASE_URL + "/logout")
                        .header("Authorization", "Bearer access-token")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout("access-token");
    }

    @Test
    @DisplayName("POST /auth/validate returns validation result")
    void validateToken_returnsBoolean() throws Exception {
        when(authService.validateToken("good-token")).thenReturn(true);

        ValidateTokenRequest request = new ValidateTokenRequest();
        request.setToken("good-token");

        mockMvc.perform(post(BASE_URL + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("GET /auth/me returns current user from JWT subject")
    void getCurrentUser_returnsUser() throws Exception {
        UserResponse response = UserResponse.builder().userId(1L).email("jane@stockpro.com").build();
        when(jwtUtil.extractUserId("access-token")).thenReturn(1L);
        when(authService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/me").header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("jane@stockpro.com"));
    }

    @Test
    @DisplayName("POST /auth/logout returns 400 for invalid authorization header")
    void logout_invalidHeader_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/logout")
                        .header("Authorization", "access-token")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /auth/profile updates the current user profile")
    void updateProfile_returnsUpdatedUser() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Jane Updated");

        UserResponse response = UserResponse.builder().userId(1L).fullName("Jane Updated").build();
        when(authService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put(BASE_URL + "/profile")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated"))
                .andExpect(jsonPath("$.data.fullName").value("Jane Updated"));
    }

    @Test
    @DisplayName("PUT /auth/change-password returns success response")
    void changePassword_returns200() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("Old@1234");
        request.setNewPassword("New@5678");

        mockMvc.perform(put(BASE_URL + "/change-password")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed"));
    }

    @Test
    @DisplayName("POST /auth/forgot-password returns success response")
    void forgotPassword_returns200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("jane@stockpro.com");

        mockMvc.perform(post(BASE_URL + "/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent to your email"));
    }

    @Test
    @DisplayName("POST /auth/reset-password returns success response")
    void resetPassword_returns200() throws Exception {
        ResetPasswordWithOtpRequest request = new ResetPasswordWithOtpRequest();
        request.setEmail("jane@stockpro.com");
        request.setOtp("123456");
        request.setNewPassword("New@5678");

        mockMvc.perform(post(BASE_URL + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful"));
    }

    @Test
    @DisplayName("GET /auth/users/{id} returns the requested user")
    @WithMockUser(roles = ADMIN_ROLE)
    void getUserById_returns200() throws Exception {
        UserResponse response = UserResponse.builder().userId(2L).email("user@stockpro.com").build();
        when(authService.getUserById(2L)).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user@stockpro.com"));
    }

    @Test
    @DisplayName("PUT /auth/admin/users/{id} returns updated user")
    @WithMockUser(roles = ADMIN_ROLE)
    void adminUpdateUser_returns200() throws Exception {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setFullName("Managed User");

        UserResponse response = UserResponse.builder().userId(2L).fullName("Managed User").build();
        when(authService.adminUpdateUser(eq(2L), any(AdminUpdateUserRequest.class), eq(1L))).thenReturn(response);

        mockMvc.perform(put(BASE_URL + "/admin/users/2")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User updated by admin"))
                .andExpect(jsonPath("$.data.fullName").value("Managed User"));
    }

    @Test
    @DisplayName("DELETE /auth/admin/users/{id} returns success response")
    @WithMockUser(roles = ADMIN_ROLE)
    void deactivateUser_returns200() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/admin/users/2")
                        .header("X-User-Id", 1)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deactivated"));
    }
}
