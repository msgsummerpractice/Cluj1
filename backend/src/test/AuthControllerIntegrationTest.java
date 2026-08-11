package com.company.events.controller;

import com.company.events.dto.LoginRequest;
import com.company.events.model.entity.User;
import com.company.events.model.enums.Role;
import com.company.events.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = User.builder()
                .email("admin@company.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        userRepository.save(user);
    }

    @Test
    void login_ValidCredentials_Returns200AndToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@company.com");
        request.setPassword("Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("admin@company.com")))
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }

    @Test
    void login_InvalidPassword_Returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@company.com");
        request.setPassword("WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_EmptyFields_Returns400() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}