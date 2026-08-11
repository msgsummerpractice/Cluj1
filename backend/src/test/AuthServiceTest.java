package com.company.events.service;

import com.company.events.dto.AuthResponse;
import com.company.events.dto.LoginRequest;
import com.company.events.model.entity.User;
import com.company.events.model.enums.Role;
import com.company.events.repository.UserRepository;
import com.company.events.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@company.com")
                .passwordHash("encodedPassword")
                .role(Role.PARTICIPANT)
                .isActive(true)
                .build();

        validLoginRequest = new LoginRequest();
        validLoginRequest.setEmail("user@company.com");
        validLoginRequest.setPassword("secretPassword");
    }

    @Test
    void login_Success() {
        when(userRepository.findByEmail("user@company.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("secretPassword", "encodedPassword")).thenReturn(true);
        when(tokenProvider.generateToken(any(User.class))).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.login(validLoginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getEmail()).isEqualTo("user@company.com");
        assertThat(response.getRole()).isEqualTo(Role.PARTICIPANT);

        verify(userRepository, times(1)).findByEmail("user@company.com");
    }

    @Test
    void login_WrongPassword_ThrowsException() {
        when(userRepository.findByEmail("user@company.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("secretPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("user@company.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void login_InactiveUser_ThrowsException() {
        testUser.setIsActive(false);
        when(userRepository.findByEmail("user@company.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }
}