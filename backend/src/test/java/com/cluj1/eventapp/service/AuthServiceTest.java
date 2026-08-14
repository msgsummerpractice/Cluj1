package com.cluj1.eventapp.service;

import com.cluj1.eventapp.dto.AuthResponse;
import com.cluj1.eventapp.dto.LogInRequest;
import com.cluj1.eventapp.mapper.UserMapper;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserService userService;
	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private UserMapper mapper;

	@Mock
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

}