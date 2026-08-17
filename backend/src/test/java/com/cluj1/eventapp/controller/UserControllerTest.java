package com.cluj1.eventapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.cluj1.eventapp.dto.UserProfileDto;
import com.cluj1.eventapp.dto.UserProfileUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cluj1.eventapp.dto.UserDTO;
import com.cluj1.eventapp.exception.EmailAlreadyRegisteredException;
import com.cluj1.eventapp.exception.GlobalExceptionHandler;
import com.cluj1.eventapp.model.enums.UserLocation;
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import com.cluj1.eventapp.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.cluj1.eventapp.config.SecurityConfig;
import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.security.JwtAuthenticationFilter;
import com.cluj1.eventapp.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        SecurityConfig.class, JwtAuthenticationFilter.class }))
@Import({ UserControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class })
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
                            ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(
                                    HttpStatus.UNAUTHORIZED)));
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private Tika tika;

    private static final List<String> ALLOWED_CONTENT_TYPE = List.of("image/png", "image/jpeg");

    private final String TEST_EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }


    @Test
    void getUsers_return200_whenUserIsAdmin() throws Exception {
        when(userService.getAllUsers(any(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/users")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
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
        UserDTO response = UserDTO.builder().id(userId).role(Role.ADMIN).isActive(true).email("user@msg.group")
                .build();
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
        UserDTO response = UserDTO.builder().id(userId).role(Role.PARTICIPANT).isActive(false)
                .email("user@msg.group")
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
    private UserRegistrationDto buildValidRegistrationDto() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail("john.doe@msg.group");
        dto.setUserLocation(UserLocation.CLUJ);
        dto.setPassword("Password1!");
        dto.setConfirmPassword("Password1!");
        return dto;
    }

    @Test
    @WithMockUser(username = TEST_EMAIL)
    void getUserProfile_ShouldReturnProfile() throws Exception {
        UserProfileDto mockProfile = new UserProfileDto();
        mockProfile.setEmail(TEST_EMAIL);
        when(userService.getUserProfileByEmail(TEST_EMAIL)).thenReturn(mockProfile);

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));

        verify(userService, times(1)).getUserProfileByEmail(TEST_EMAIL);
    }

    @Test
    @WithMockUser(username = TEST_EMAIL)
    void updateUserProfile_WithValidImage_ShouldReturnOk() throws Exception {
        byte[] pngBytes = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
        MockMultipartFile file = new MockMultipartFile(
                "profilePicture", "test.png", MediaType.IMAGE_PNG_VALUE, pngBytes
        );

        when(tika.detect(any(InputStream.class))).thenReturn("image/png");

        MockMultipartHttpServletRequestBuilder builder =
                MockMvcRequestBuilders.multipart("/api/users/profile");
        builder.with(request -> {
            request.setMethod(HttpMethod.PATCH.name());
            return request;
        });


        mockMvc.perform(builder.file(file))
                .andExpect(status().isOk());

        verify(userService, times(1)).updateUserProfile(eq(TEST_EMAIL), any(UserProfileUpdateDto.class));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL)
    void updateUserProfile_WithInvalidFileType_ShouldReturn415() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "profilePicture", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "dummy pdf content".getBytes()
        );

        when(tika.detect(any(InputStream.class))).thenReturn("application/pdf");

        MockMultipartHttpServletRequestBuilder builder =
                MockMvcRequestBuilders.multipart("/api/users/profile");
        builder.with(request -> {
            request.setMethod(HttpMethod.PATCH.name());
            return request;
        });


        mockMvc.perform(builder.file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().string("Image should be in PNG or JPEG format!"));

        verify(userService, never()).updateUserProfile(anyString(), any(UserProfileUpdateDto.class));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL)
    void updateUserProfile_WithoutImage_ShouldReturnOk() throws Exception {

        MockMultipartHttpServletRequestBuilder builder =
                MockMvcRequestBuilders.multipart("/api/users/profile");
        builder.with(request -> {
            request.setMethod(HttpMethod.PATCH.name());
            return request;
        });

        mockMvc.perform(builder)
                .andExpect(status().isOk());

        verify(tika, never()).detect(any(InputStream.class));
        verify(userService, times(1)).updateUserProfile(eq(TEST_EMAIL), any(UserProfileUpdateDto.class));
    }
}