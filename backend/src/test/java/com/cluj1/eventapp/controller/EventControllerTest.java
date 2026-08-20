package com.cluj1.eventapp.controller;

import com.cluj1.eventapp.config.SecurityConfig;
import com.cluj1.eventapp.dto.EventDto;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;
import com.cluj1.eventapp.security.JwtAuthenticationFilter;
import com.cluj1.eventapp.service.EventDetailsService;
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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        SecurityConfig.class, JwtAuthenticationFilter.class }))
@Import(EventControllerTest.TestSecurityConfig.class)
class EventControllerTest {

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

    @Test
    void getAllEventsReturnOkForMarketingOrganizer() throws Exception {
        when(eventService.getAllEvents()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/events")
                .with(user("marketingUser").authorities(new SimpleGrantedAuthority("MARKETING_ORGANIZER")))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getAllEventsReturnOkForHrUser() throws Exception {
        when(eventService.getAllEvents()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/events")
                .with(user("hrUser").authorities(new SimpleGrantedAuthority("HR_USER"))))
                .andExpect(status().isOk());
    }

    @Test
    void getAllEventsReturnForbiddenForParticipantRole() throws Exception {
        mockMvc.perform(get("/api/events")
                .with(user("regularUser").authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllEventsReturnUnauthorizedForUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllEventsMapAllDtoFieldsCorrectly() throws Exception {
        EventDto dto = EventDto.builder()
                .id(UUID.randomUUID())
                .name("Summer Fest")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.DRAFT)
                .startDate(OffsetDateTime.parse("2026-09-01T10:00:00+00:00"))
                .endDate(OffsetDateTime.parse("2026-09-01T18:00:00+00:00"))
                .build();

        when(eventService.getAllEvents()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/events")
                .with(user("marketingUser").authorities(new SimpleGrantedAuthority("MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].name").value("Summer Fest"))
                .andExpect(jsonPath("$[0].location").value("CLUJ"))
                .andExpect(jsonPath("$[0].type").value("LOCAL"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    void getEligibleEventsReturns200ForParticipant() throws Exception {
        when(eventService.getEligibleEventsForCurrentUser()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/events/eligible")
                .with(user("participant").authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getEligibleEventsReturns401ForUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/events/eligible"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEligibleEventsIncludesRegistrationAndCheckInFieldsInResponse() throws Exception {
        EventDto registered = EventDto.builder()
                .id(UUID.randomUUID())
                .name("ClujFest")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.PUBLISHED)
                .isRegistered(true)
                .isCheckedIn(false)
                .build();
        EventDto checkedIn = EventDto.builder()
                .id(UUID.randomUUID())
                .name("Tech Summit")
                .location(EventLocation.ALL)
                .type(EventType.INTERNAL)
                .status(EventStatus.PUBLISHED)
                .isRegistered(true)
                .isCheckedIn(true)
                .build();

        when(eventService.getEligibleEventsForCurrentUser()).thenReturn(List.of(registered, checkedIn));

        mockMvc.perform(get("/api/events/eligible")
                .with(user("participant").authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isRegistered").value(true))
                .andExpect(jsonPath("$[0].isCheckedIn").value(false))
                .andExpect(jsonPath("$[1].isRegistered").value(true))
                .andExpect(jsonPath("$[1].isCheckedIn").value(true));
    }
}