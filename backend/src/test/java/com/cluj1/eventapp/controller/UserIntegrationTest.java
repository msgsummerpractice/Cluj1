package com.cluj1.eventapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.UserDetails;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.model.enums.UserLocation;
import com.cluj1.eventapp.repository.UserRepository;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@EnableMethodSecurity
@AutoConfigureMockMvc
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();

        User adminUser = User.builder()
                .email("integration.admin@msg.group")
                .passwordHash("hash")
                .role(Role.valueOf("ADMIN"))
                .isActive(true)
                .build();

        UserDetails adminDetails = UserDetails.builder()
                .user(adminUser)
                .firstName("IntAdmin")
                .lastName("IntSuprem")
                .location(UserLocation.valueOf("CLUJ"))
                .build();
        adminUser.setUserDetails(adminDetails);

        User participantUser = User.builder()
                .email("integration.participant@msg.group")
                .passwordHash("hash")
                .role(Role.valueOf("PARTICIPANT"))
                .isActive(true)
                .build();

        UserDetails participantDetails = UserDetails.builder()
                .user(participantUser)
                .firstName("IntAndrei")
                .lastName("IntPopescu")
                .location(UserLocation.valueOf("TIMISOARA"))
                .build();
        participantUser.setUserDetails(participantDetails);

        userRepository.saveAll(List.of(adminUser, participantUser));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldReturnAllUsers_WhenNoSearchTermIsProvided() throws Exception {
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].email",
                        hasItems("integration.admin@msg.group",
                                "integration.participant@msg.group")));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldReturnFilteredUsers_WhenSearchTermIsProvided() throws Exception {
        mockMvc.perform(get("/api/users")
                .param("search", "timisoara")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email", is("integration.participant@msg.group")))
                .andExpect(jsonPath("$.content[0].location", is("TIMISOARA")));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldReturnFilteredUsers_WhenRoleIsProvided() throws Exception {
        mockMvc.perform(get("/api/users")
                .param("search", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].role", is("ADMIN")))
                .andExpect(jsonPath("$.content[0].firstName", is("IntAdmin")));
    }

    @Test
    @WithMockUser(authorities = "PARTICIPANT")
    void shouldReturn403_WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserProfile_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "integration.participant@msg.group")
    void updateUserProfile_Integration_SuccessFlow() throws Exception {
        byte[] pngBytes = new byte[] { (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };

        MockMultipartFile realFile = new MockMultipartFile(
                "profilePicture", "avatar.png", MediaType.IMAGE_PNG_VALUE, pngBytes
        );

        MockMultipartHttpServletRequestBuilder builder =
                MockMvcRequestBuilders.multipart("/api/users/profile");
        builder.with(request -> {
            request.setMethod(HttpMethod.PATCH.name());
            return request;
        });

        mockMvc.perform(builder.file(realFile))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401_WhenUserIsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
        }

}