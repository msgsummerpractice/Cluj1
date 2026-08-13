package com.cluj1.eventapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.UUID;

import com.cluj1.eventapp.dto.UserDTO;
import com.cluj1.eventapp.exception.EmailAlreadyRegisteredException;
import com.cluj1.eventapp.model.enums.Role;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.cluj1.eventapp.config.SecurityConfig;
import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.security.JwtAuthenticationFilter;
import com.cluj1.eventapp.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        SecurityConfig.class, JwtAuthenticationFilter.class }))
@Import(UserControllerTest.TestSecurityConfig.class)
class UserControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/users/register").permitAll()
                            .anyRequest().authenticated())
                    .exceptionHandling(
                            ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @Test
    void getUsers_return200_whenUserIsAdmin() throws Exception {
        when(userService.getAllUsers(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getUsers_return403_whenUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/api/users")
                .with(user("user").authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsers_return401_whenUserIsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerUser_validInput_returns200AndEmptyBody() throws Exception {
        UserRegistrationDto validDto = new UserRegistrationDto();
        validDto.setFirstName("John");
        validDto.setLastName("Doe");
        validDto.setEmail("john.doe@msg.group");
        validDto.setUserLocation(com.cluj1.eventapp.model.enums.UserLocation.CLUJ);
        validDto.setPassword("Password1!");
        validDto.setConfirmPassword("Password1!");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(userService).registerUser(any(UserRegistrationDto.class));
    }

    @Test
    void registerUser_PasswordsDoNotMatch() throws Exception {
        UserRegistrationDto invalidDto = new UserRegistrationDto();
        invalidDto.setPassword("password123");
        invalidDto.setConfirmPassword("differentPassword");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void registerUser_ServiceThrowsEmailAlreadyRegistered() throws Exception {
        UserRegistrationDto validDto = new UserRegistrationDto();
        validDto.setFirstName("John");
        validDto.setLastName("Doe");
        validDto.setEmail("john.doe@msg.group");
        validDto.setUserLocation(com.cluj1.eventapp.model.enums.UserLocation.CLUJ);
        validDto.setPassword("Password1!");
        validDto.setConfirmPassword("Password1!");

        doThrow(new EmailAlreadyRegisteredException())
                .when(userService).registerUser(any(UserRegistrationDto.class));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isConflict());

        verify(userService).registerUser(any(UserRegistrationDto.class));
    }

    @Test
    void updateUserRole_return200_whenUserIsAdmin() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDTO response = UserDTO.builder().id(userId).role(Role.ADMIN).isActive(true).email("user@msg.group").build();
        when(userService.updateUserRole(any(UUID.class), any(Role.class))).thenReturn(response);

        mockMvc.perform(patch("/api/users/" + userId + "/role")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void updateUserRole_return403_whenUserIsNotAdmin() throws Exception {
        mockMvc.perform(patch("/api/users/" + UUID.randomUUID() + "/role")
                .with(user("user").authorities(new SimpleGrantedAuthority("PARTICIPANT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void updateUserRole_return401_whenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/users/" + UUID.randomUUID() + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUserRole_return404_whenUserNotFound() throws Exception {
        when(userService.updateUserRole(any(UUID.class), any(Role.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(patch("/api/users/" + UUID.randomUUID() + "/role")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUserRole_return400_whenRemovingLastAdminRole() throws Exception {
        when(userService.updateUserRole(any(UUID.class), any(Role.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot remove or deactivate the last active Admin account."));

        mockMvc.perform(patch("/api/users/" + UUID.randomUUID() + "/role")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"PARTICIPANT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserStatus_return200_whenUserIsAdmin() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDTO response = UserDTO.builder().id(userId).role(Role.PARTICIPANT).isActive(false).email("user@msg.group")
                .build();
        when(userService.updateUserStatus(any(UUID.class), anyBoolean())).thenReturn(response);

        mockMvc.perform(patch("/api/users/" + userId + "/status")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .param("isActive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void updateUserStatus_return403_whenUserIsNotAdmin() throws Exception {
        mockMvc.perform(patch("/api/users/" + UUID.randomUUID() + "/status")
                .with(user("user").authorities(new SimpleGrantedAuthority("PARTICIPANT")))
                .param("isActive", "false"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void updateUserStatus_return401_whenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/users/" + UUID.randomUUID() + "/status")
                .param("isActive", "false"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUserStatus_return404_whenUserNotFound() throws Exception {
        when(userService.updateUserStatus(any(UUID.class), anyBoolean()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(patch("/api/users/" + UUID.randomUUID() + "/status")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .param("isActive", "false"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUserStatus_return400_whenDeactivatingLastAdmin() throws Exception {
        when(userService.updateUserStatus(any(UUID.class), anyBoolean()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot remove or deactivate the last active Admin account."));

        mockMvc.perform(patch("/api/users/" + UUID.randomUUID() + "/status")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .param("isActive", "false"))
                .andExpect(status().isBadRequest());
    }
}
