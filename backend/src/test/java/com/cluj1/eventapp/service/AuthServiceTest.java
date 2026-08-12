package com.cluj1.eventapp.service;

import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.exception.EmailAlreadyRegisteredException;
import com.cluj1.eventapp.mapper.DtoMapper;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.repository.UserRepository;
import com.cluj1.eventapp.security.JwtTokenProvider;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;
    private PasswordEncoder passwordEncoder;

    @Mock
    private DtoMapper mapper;
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LogInRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("encodedPassword")
                .role(Role.PARTICIPANT)
                .isActive(true)
                .build();

        validLoginRequest = new LogInRequest();
        validLoginRequest.setEmail("user@example.com");
        validLoginRequest.setPassword("secretPassword");
    }

    @Test
    void login_validCredentials_returnsAuthResponseWithToken() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("secretPassword", "encodedPassword")).thenReturn(true);
        when(tokenProvider.generateToken(any(User.class))).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.login(validLoginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");

        verify(userRepository, times(1)).findByEmail("user@example.com");
    }
    @Test
    void login_wrongPassword_throwsException() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("secretPassword", "encodedPassword")).thenReturn(false);
        UserRegistrationDto dto = mock(UserRegistrationDto.class);
        when(dto.getEmail()).thenReturn(email);
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }
    @Test
    void login_userNotFound_throwsException() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }
    @Test
    void login_inactiveUser_throwsException() {
        testUser.setIsActive(false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }
    @Test
    void registerUser_ShouldSaveUserWhenEmailIsNotRegistered() {
        String email = "test@example.com";

        UserRegistrationDto dto = mock(UserRegistrationDto.class);
        when(dto.getEmail()).thenReturn(email);

        User mappedUser = new User();

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(mapper.mapToEntity(dto)).thenReturn(mappedUser);

        authService.registerUser(dto);
        verify(userRepository, times(1)).save(mappedUser);

        }

    @Test
    void registerUser_ShouldThrowExceptionWhenEmailIsAlreadyRegistered() {

        String email = "duplicate@example.com";

        UserRegistrationDto dto = mock(UserRegistrationDto.class);
        when(dto.getEmail()).thenReturn(email);

        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> {
            authService.registerUser(dto);
        });

        verify(mapper, never()).mapToEntity(any());
        verify(userRepository, never()).save(any());
    }
}