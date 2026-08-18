package com.cluj1.eventapp.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.EventDetails;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.repository.EventDetailsRepository;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class EventDetailsIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EventDetailsRepository eventDetailsRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User savedUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        eventDetailsRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        savedUser = userRepository.save(User.builder()
                .email("eventdetails.test@msg.group")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.MARKETING_ORGANIZER)
                .isActive(true)
                .build());
    }

    private Event saveEvent(String name) {
        return eventRepository.save(Event.builder()
                .name(name)
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.DRAFT)
                .createdBy(savedUser)
                .build());
    }

    @Test
    void getEventDetailsByIdReturnPersistedEventDetailsForAuthenticatedUser() throws Exception {
        Event event = saveEvent("Details Integration Event");

        EventDetails savedDetails = eventDetailsRepository.save(EventDetails.builder()
                .event(event)
                .description("Integration description")
                .foodProvided(true)
                .qrCodeContent("integration-qr")
                .eventCode("IN45TG")
                .build());

        mockMvc.perform(get("/api/event-details/{id}", savedDetails.getId())
                .with(user("marketingUser").roles("MARKETING_ORGANIZER")))
                .andExpect(status().isOk());
    }

    @Test
    void getEventDetailsByIdReturnUnauthorizedForUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/event-details/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEventDetailsByIdReturnInternalServerErrorWhenEventDetailsDoNotExist() throws Exception {
        mockMvc.perform(get("/api/event-details/{id}", UUID.randomUUID())
                .with(user("hrUser").roles("HR_USER")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."));
    }
}