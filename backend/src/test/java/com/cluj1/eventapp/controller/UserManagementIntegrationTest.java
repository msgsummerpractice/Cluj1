package com.cluj1.eventapp.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import com.cluj1.eventapp.dto.UpdateRoleRequest;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserManagementIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID adminId;
    private UUID secondAdminId;
    private UUID participantId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        userRepository.deleteAll();

        User admin = userRepository.save(User.builder()
                .email("admin.one@msg.group").passwordHash("hash")
                .role(Role.ADMIN).isActive(true).build());

        User secondAdmin = userRepository.save(User.builder()
                .email("admin.two@msg.group").passwordHash("hash")
                .role(Role.ADMIN).isActive(true).build());

        User participant = userRepository.save(User.builder()
                .email("participant.one@msg.group").passwordHash("hash")
                .role(Role.PARTICIPANT).isActive(true).build());

        adminId = admin.getId();
        secondAdminId = secondAdmin.getId();
        participantId = participant.getId();
    }

    @Test
    void updateUserStatus_return200AndDeactivateUser() throws Exception {
        mockMvc.perform(patch("/api/users/" + participantId + "/status")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .param("isActive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        assertFalse(userRepository.findById(participantId).orElseThrow().getIsActive());
    }

    @Test
    void updateUserStatus_return200AndActivateUser() throws Exception {
        User participant = userRepository.findById(participantId).orElseThrow();
        participant.setIsActive(false);
        userRepository.save(participant);

        mockMvc.perform(patch("/api/users/" + participantId + "/status")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));

        assertTrue(userRepository.findById(participantId).orElseThrow().getIsActive());
    }

    @Test
    void updateUserStatus_return404_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(patch("/api/users/" + UUID.randomUUID() + "/status")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .param("isActive", "false"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUserStatus_return400_whenDeactivatingLastActiveAdmin() throws Exception {
        userRepository.deleteById(secondAdminId);

        mockMvc.perform(patch("/api/users/" + adminId + "/status")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .param("isActive", "false"))
                .andExpect(status().isBadRequest());

        assertTrue(userRepository.findById(adminId).orElseThrow().getIsActive());
    }

    @Test
    void updateUserStatus_return403_whenCallerIsNotAdmin() throws Exception {
        mockMvc.perform(patch("/api/users/" + participantId + "/status")
                .with(user("user").authorities(new SimpleGrantedAuthority("PARTICIPANT")))
                .param("isActive", "false"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserStatus_return401_whenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/users/" + participantId + "/status")
                .param("isActive", "false"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUserRole_return200AndChangeRole() throws Exception {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole(Role.ADMIN);

        mockMvc.perform(patch("/api/users/" + participantId + "/role")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        assertEquals(Role.ADMIN, userRepository.findById(participantId).orElseThrow().getRole());
    }

    @Test
    void updateUserRole_return404_whenUserDoesNotExist() throws Exception {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole(Role.ADMIN);

        mockMvc.perform(patch("/api/users/" + UUID.randomUUID() + "/role")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUserRole_return400_whenRemovingRoleFromLastActiveAdmin() throws Exception {
        userRepository.deleteById(secondAdminId);

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole(Role.PARTICIPANT);

        mockMvc.perform(patch("/api/users/" + adminId + "/role")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertEquals(Role.ADMIN, userRepository.findById(adminId).orElseThrow().getRole());
    }

    @Test
    void updateUserRole_return403_whenCallerIsNotAdmin() throws Exception {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole(Role.ADMIN);

        mockMvc.perform(patch("/api/users/" + participantId + "/role")
                .with(user("user").authorities(new SimpleGrantedAuthority("PARTICIPANT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserRole_return401_whenUnauthenticated() throws Exception {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole(Role.ADMIN);

        mockMvc.perform(patch("/api/users/" + participantId + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
