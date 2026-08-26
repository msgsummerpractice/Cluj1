package com.cluj1.eventapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.cluj1.eventapp.config.SecurityConfig;
import com.cluj1.eventapp.dto.AttendanceReportExcelRowDto;
import com.cluj1.eventapp.exception.GlobalExceptionHandler;
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.security.JwtAuthenticationFilter;
import com.cluj1.eventapp.service.RegistrationService;

@WebMvcTest(controllers = RegistrationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        SecurityConfig.class, JwtAuthenticationFilter.class }))
@Import({ RegistrationControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class })
class RegistrationControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .exceptionHandling(ex -> ex.authenticationEntryPoint(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private RegistrationRepository registrationRepository;

    private static final String TEST_EMAIL = "john.doe@msg.group";

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getRegistrationsCount_returns200AndCount_whenAuthenticated() throws Exception {
        when(registrationService.getRegistrationsPerUserByEmail(TEST_EMAIL)).thenReturn(7);

        mockMvc.perform(get("/api/registration/count")
                .with(user(TEST_EMAIL).authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(7));

        verify(registrationService).getRegistrationsPerUserByEmail(TEST_EMAIL);
    }

    @Test
    void getRegistrationsCount_returnsZero_whenUserHasNoRegistrations() throws Exception {
        when(registrationService.getRegistrationsPerUserByEmail(TEST_EMAIL)).thenReturn(0);

        mockMvc.perform(get("/api/registration/count")
                .with(user(TEST_EMAIL).authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    @Test
    void getRegistrationsCount_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/registration/count"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(registrationService);
    }

    @Test
    void previewReport_returns200AndRows_whenAuthenticated() throws Exception {
        UUID eventId = UUID.randomUUID();
        AttendanceReportExcelRowDto row = AttendanceReportExcelRowDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email(TEST_EMAIL)
                .hasGdprConsent(true)
                .isPresent(true)
                .registrationDate(OffsetDateTime.now())
                .build();
        when(registrationRepository.findAttendanceReportRows(eventId)).thenReturn(List.of(row));

        mockMvc.perform(get("/api/registration/api/events/" + eventId + "/attendance-report/preview")
                .with(user("hr").authorities(new SimpleGrantedAuthority("HR_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(TEST_EMAIL))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].hasGdprConsent").value(true));

        verify(registrationRepository).findAttendanceReportRows(eventId);
    }

    @Test
    void previewReport_returnsEmptyArray_whenNoRegistrations() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(registrationRepository.findAttendanceReportRows(any(UUID.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/registration/api/events/" + eventId + "/attendance-report/preview")
                .with(user("hr").authorities(new SimpleGrantedAuthority("HR_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void previewReport_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/registration/api/events/" + UUID.randomUUID() + "/attendance-report/preview"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(registrationRepository);
    }
}


