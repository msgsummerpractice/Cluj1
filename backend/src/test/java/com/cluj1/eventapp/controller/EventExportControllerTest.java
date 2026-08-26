package com.cluj1.eventapp.controller;

import com.cluj1.eventapp.config.SecurityConfig;
import com.cluj1.eventapp.exception.InvalidEventOperationException;
import com.cluj1.eventapp.mapper.EventMapper;
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.security.JwtAuthenticationFilter;
import com.cluj1.eventapp.service.AttendanceExcelGeneratorService;
import com.cluj1.eventapp.service.EventCheckInService;
import com.cluj1.eventapp.service.EventDetailsService;
import com.cluj1.eventapp.service.EventExportService;
import com.cluj1.eventapp.service.EventService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EventController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        SecurityConfig.class, JwtAuthenticationFilter.class }))
@Import(EventExportControllerTest.TestSecurityConfig.class)
class EventExportControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .exceptionHandling(
                            ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;
    @MockitoBean
    private EventDetailsService eventDetailsService;
    @MockitoBean
    private EventCheckInService eventCheckInService;
    @MockitoBean
    private EventExportService eventExportService;
    @MockitoBean
    private RegistrationRepository registrationRepository;
    @MockitoBean
    private AttendanceExcelGeneratorService attendanceReportExcelGenerator;
    @MockitoBean
    private EventMapper eventMapper;

    private static final String EXPORT_URL = "/api/events/{id}/export";

    @Test
    void exportReturnsForbiddenForParticipant() throws Exception {
        mockMvc.perform(get(EXPORT_URL, UUID.randomUUID())
                .with(user("participant").authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportReturnsForbiddenForHrUser() throws Exception {
        mockMvc.perform(get(EXPORT_URL, UUID.randomUUID())
                .with(user("hrUser").authorities(new SimpleGrantedAuthority("HR_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get(EXPORT_URL, UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportReturnsOkForMarketingOrganizer() throws Exception {
        when(eventExportService.exportEventRegistrationsToExcel(any())).thenReturn(new byte[] { 1, 2, 3 });

        mockMvc.perform(get(EXPORT_URL, UUID.randomUUID())
                .with(user("organizer").authorities(new SimpleGrantedAuthority("MARKETING_ORGANIZER"))))
                .andExpect(status().isOk());
    }

    @Test
    void exportReturnsExcelContentType() throws Exception {
        when(eventExportService.exportEventRegistrationsToExcel(any())).thenReturn(new byte[] { 1, 2, 3 });

        mockMvc.perform(get(EXPORT_URL, UUID.randomUUID())
                .with(user("organizer").authorities(new SimpleGrantedAuthority("MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void exportReturnsAttachmentContentDispositionWithEventId() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(eventExportService.exportEventRegistrationsToExcel(eventId)).thenReturn(new byte[] { 1, 2, 3 });

        mockMvc.perform(get(EXPORT_URL, eventId)
                .with(user("organizer").authorities(new SimpleGrantedAuthority("MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"attendance_report_" + eventId + ".xlsx\""));
    }

    @Test
    void exportReturnsExcelBytesAsBody() throws Exception {
        byte[] fakeExcel = { 0x50, 0x4B, 0x03, 0x04 };
        when(eventExportService.exportEventRegistrationsToExcel(any())).thenReturn(fakeExcel);

        mockMvc.perform(get(EXPORT_URL, UUID.randomUUID())
                .with(user("organizer").authorities(new SimpleGrantedAuthority("MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andExpect(content().bytes(fakeExcel));
    }

    @Test
    void exportReturnsBadRequestWhenEventIsInDraftStatus() throws Exception {
        when(eventExportService.exportEventRegistrationsToExcel(any()))
                .thenThrow(new InvalidEventOperationException("Cannot export data for events in DRAFT status."));

        mockMvc.perform(get(EXPORT_URL, UUID.randomUUID())
                .with(user("organizer").authorities(new SimpleGrantedAuthority("MARKETING_ORGANIZER"))))
                .andExpect(status().isBadRequest());
    }
}
