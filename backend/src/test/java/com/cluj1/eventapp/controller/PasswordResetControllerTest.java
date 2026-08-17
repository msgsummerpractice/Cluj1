package com.cluj1.eventapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cluj1.eventapp.config.SecurityConfig;
import com.cluj1.eventapp.dto.ForgotPasswordRequest;
import com.cluj1.eventapp.dto.ResetPasswordRequest;
import com.cluj1.eventapp.security.JwtAuthenticationFilter;
import com.cluj1.eventapp.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = PasswordResetController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        SecurityConfig.class, JwtAuthenticationFilter.class }))
@Import(PasswordResetControllerTest.TestSecurityConfig.class)
class PasswordResetControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                            .anyRequest().authenticated())
                    .exceptionHandling(
                            ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(
                                    HttpStatus.UNAUTHORIZED)));
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void forgotPassword_validRequest_returns200AndLowercasesEmail() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("USER@EXAMPLE.COM");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset token sent to email"));

        verify(passwordResetService).createPasswordResetToken("user@example.com");
    }

    @Test
    void forgotPassword_invalidEmail_returns400() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("not-an-email");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passwordResetService);
    }

    @Test
    void resetPassword_validRequest_returns200() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("token-123", "Password1!", "Password1!");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset successful"));

        verify(passwordResetService).resetPassword("token-123", "Password1!", "Password1!");
    }

    @Test
    void resetPassword_serviceValidationFailure_returns400WithMessage() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("token-123", "Password1!", "Password1!");
        doThrow(new IllegalArgumentException("Invalid token"))
                .when(passwordResetService)
                .resetPassword(any(), any(), any());

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid token"));
    }

    @Test
    void resetPassword_invalidPasswordFormat_returns400() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("token-123", "short", "short");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passwordResetService);
    }
}