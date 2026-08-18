package com.cluj1.eventapp.controller;

import com.cluj1.eventapp.config.SecurityConfig;
import com.cluj1.eventapp.dto.CheckInRequest;
import com.cluj1.eventapp.exception.GlobalExceptionHandler;
import com.cluj1.eventapp.exception.InvalidEventOperationException;
import com.cluj1.eventapp.model.enums.CheckInMethod;
import com.cluj1.eventapp.security.JwtAuthenticationFilter;
import com.cluj1.eventapp.service.EventCheckInService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventCheckInController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                SecurityConfig.class, JwtAuthenticationFilter.class }))
@Import({ EventCheckInControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class })
class EventCheckInControllerTest {

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

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private EventCheckInService checkInService;

        @BeforeEach
        void setUp() {
                this.mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        }

        private CheckInRequest buildRequest(String code, CheckInMethod method) {
                CheckInRequest req = new CheckInRequest();
                req.setMethod(method);
                if (method == CheckInMethod.QR) {
                        req.setEventId(UUID.fromString(code));
                } else {
                        req.setEventCode(code);
                }
                return req;
        }

        @Test
        void checkIn_unauthenticated_returns401() throws Exception {
                mockMvc.perform(post("/api/events/checkin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buildRequest("ABC123", CheckInMethod.MANUAL))))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void checkIn_blankCode_returns400() throws Exception {
                CheckInRequest req = buildRequest("", CheckInMethod.MANUAL);

                mockMvc.perform(post("/api/events/checkin")
                                .with(user("john.doe@msg.group").roles("PARTICIPANT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void checkIn_codeTooShort_returns400() throws Exception {
                CheckInRequest req = buildRequest("AB1", CheckInMethod.MANUAL);

                mockMvc.perform(post("/api/events/checkin")
                                .with(user("john.doe@msg.group").roles("PARTICIPANT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void checkIn_nullMethod_returns400() throws Exception {
                CheckInRequest req = new CheckInRequest();
                req.setEventCode("ABC123");
                req.setMethod(null);

                mockMvc.perform(post("/api/events/checkin")
                                .with(user("john.doe@msg.group").roles("PARTICIPANT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void checkIn_validRequest_returns200() throws Exception {
                doNothing().when(checkInService).processCheckIn(eq("john.doe@msg.group"), any());

                mockMvc.perform(post("/api/events/checkin")
                                .with(user("john.doe@msg.group").roles("PARTICIPANT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buildRequest("ABC123", CheckInMethod.MANUAL))))
                                .andExpect(status().isOk());
        }

        @Test
        void checkIn_eventNotFound_returns404() throws Exception {
                doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "checkin.error.event.notfound"))
                                .when(checkInService).processCheckIn(any(), any());

                mockMvc.perform(post("/api/events/checkin")
                                .with(user("john.doe@msg.group").roles("PARTICIPANT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buildRequest("ABC123", CheckInMethod.MANUAL))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("checkin.error.event.notfound"));
        }

        @Test
        void checkIn_userNotRegistered_returns400() throws Exception {
                doThrow(new InvalidEventOperationException("checkin.error.user.notregistered"))
                                .when(checkInService).processCheckIn(any(), any());

                mockMvc.perform(post("/api/events/checkin")
                                .with(user("john.doe@msg.group").roles("PARTICIPANT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buildRequest("ABC123", CheckInMethod.MANUAL))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("checkin.error.user.notregistered"));
        }

        @Test
        void checkIn_alreadyCheckedIn_returns400() throws Exception {
                doThrow(new InvalidEventOperationException("checkin.error.user.alreadycheckedin"))
                                .when(checkInService).processCheckIn(any(), any());

                mockMvc.perform(post("/api/events/checkin")
                                .with(user("john.doe@msg.group").roles("PARTICIPANT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buildRequest("ABC123", CheckInMethod.MANUAL))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("checkin.error.user.alreadycheckedin"));
        }

        @Test
        void checkIn_eventExpired_returns400() throws Exception {
                doThrow(new InvalidEventOperationException("checkin.error.event.expired"))
                                .when(checkInService).processCheckIn(any(), any());

                mockMvc.perform(post("/api/events/checkin")
                                .with(user("john.doe@msg.group").roles("PARTICIPANT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buildRequest("ABC123", CheckInMethod.MANUAL))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("checkin.error.event.expired"));
        }
}
