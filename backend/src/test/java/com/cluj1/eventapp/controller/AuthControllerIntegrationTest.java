package com.cluj1.eventapp.controller;

import com.cluj1.eventapp.dto.LogInRequest;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PasswordEncoder passwordEncoder;


    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();

        User user = User.builder()
                .email("admin.test@msg.group")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        userRepository.save(user);
    }

    @Test
    void registerUser_IntegrationSuccess() throws Exception {
        UserRegistrationDto dto = createUserRegistrationDto("integration.user@msg.group", "Password123!");

        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        assertTrue(userRepository.findByEmail("integration.user@msg.group").isPresent());
    }
    @Test
    void login_validCredentials_returns200AndToken() throws Exception {
        LogInRequest request = createLoginRequest("admin.test@msg.group", "Password123!");

        mockMvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    void login_invalidPassword_returns401() throws Exception {
        LogInRequest request = createLoginRequest("admin.test@msg.group", "WrongPassword");

        mockMvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_emptyFields_returns400() throws Exception {
        LogInRequest request = createLoginRequest("", "");

        mockMvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    private LogInRequest createLoginRequest(String email, String password) {
        LogInRequest request = new LogInRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
    private UserRegistrationDto createUserRegistrationDto(String email, String password) {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail(email);
        dto.setPassword(password);
        dto.setConfirmPassword(password);
        dto.setFirstName("Integration");
        dto.setLastName("User");
        dto.setUserLocation(com.cluj1.eventapp.model.enums.UserLocation.CLUJ);
        return dto;
    }
}