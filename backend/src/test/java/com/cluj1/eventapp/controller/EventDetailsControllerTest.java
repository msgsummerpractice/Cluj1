package com.cluj1.eventapp.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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

import com.cluj1.eventapp.config.SecurityConfig;
import com.cluj1.eventapp.model.EventDetails;
import com.cluj1.eventapp.security.JwtAuthenticationFilter;
import com.cluj1.eventapp.service.EventDetailsService;

@WebMvcTest(controllers = EventDetailsController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        SecurityConfig.class, JwtAuthenticationFilter.class }))
@Import(EventDetailsControllerTest.TestSecurityConfig.class)
class EventDetailsControllerTest {

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
    private EventDetailsService eventDetailsService;

    @Test
    void getEventDetailsByIdReturnOkForAuthenticatedUser() throws Exception {
        UUID id = UUID.randomUUID();
        EventDetails eventDetails = EventDetails.builder()
                .id(id)
                .description("Event details description")
                .foodProvided(true)
                .qrCodeContent("qr-content")
                .eventCode("ZX12CV")
                .build();

        when(eventDetailsService.getEventDetailsById(id)).thenReturn(eventDetails);

        mockMvc.perform(get("/api/event-details/{id}", id)
                .with(user("marketingUser").roles("MARKETING_ORGANIZER"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.description").value("Event details description"))
                .andExpect(jsonPath("$.foodProvided").value(true))
                .andExpect(jsonPath("$.qrCodeContent").value("qr-content"))
                .andExpect(jsonPath("$.eventCode").value("ZX12CV"));
    }

    @Test
    void getEventDetailsByIdReturnUnauthorizedForUnauthenticatedUser() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/event-details/{id}", id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEventDetailsByIdReturnInternalServerErrorWhenServiceThrows() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventDetailsService.getEventDetailsById(id))
                .thenThrow(new RuntimeException("Event details not found for id: " + id));

        mockMvc.perform(get("/api/event-details/{id}", id)
                .with(user("hrUser").roles("HR_USER")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."));
    }
}