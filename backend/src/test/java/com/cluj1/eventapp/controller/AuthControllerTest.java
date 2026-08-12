package com.cluj1.eventapp.controller;

import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private UserRegistrationDto validDto;

    @BeforeEach
    void setUp() {
        validDto = new UserRegistrationDto();
        validDto.setPassword("password123");
        validDto.setConfirmPassword("password123");
    }

    @Test
    void registerUser_Success() {
        ResponseEntity<?> response = authController.registerUser(validDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals("User registered successfully!", body.get("message"));

        verify(authService).registerUser(validDto);
    }

    @Test
    void registerUser_PasswordsDoNotMatch() {
        validDto.setConfirmPassword("differentPassword");

        ResponseEntity<?> response = authController.registerUser(validDto);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Passwords do not match", response.getBody());

        verifyNoInteractions(authService);
    }

    @Test
    void registerUser_ServiceThrowsIllegalArgumentException() {
        doThrow(new IllegalArgumentException("Email already in use"))
                .when(authService).registerUser(validDto);

        ResponseEntity<?> response = authController.registerUser(validDto);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals("Email already in use", body.get("error"));

        verify(authService).registerUser(validDto);
    }
}