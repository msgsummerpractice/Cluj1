package com.cluj1.eventapp.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.UserDetails;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.model.enums.UserLocation;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User participant;
    private User organizer;
    private Event event;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        registrationRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        participant = User.builder()
                .email("john.doe@msg.group")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.PARTICIPANT)
                .isActive(true)
                .build();
        UserDetails participantDetails = UserDetails.builder()
                .firstName("John")
                .lastName("Doe")
                .location(UserLocation.CLUJ)
                .user(participant)
                .build();
        participant.setUserDetails(participantDetails);
        userRepository.save(participant);

        organizer = User.builder()
                .email("jane.smith@msg.group")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.MARKETING_ORGANIZER)
                .isActive(true)
                .build();
        UserDetails organizerDetails = UserDetails.builder()
                .firstName("Jane")
                .lastName("Smith")
                .location(UserLocation.CLUJ)
                .user(organizer)
                .build();
        organizer.setUserDetails(organizerDetails);
        userRepository.save(organizer);

        event = Event.builder()
                .name("Integration Event")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.PUBLISHED)
                .eventStartDate(OffsetDateTime.now().plusDays(3))
                .eventEndTime(OffsetDateTime.now().plusDays(3).plusHours(2))
                .registrationEndDate(OffsetDateTime.now().plusDays(2))
                .createdBy(organizer)
                .build();
        eventRepository.save(event);

        Registration registration = Registration.builder()
                .user(participant)
                .event(event)
                .gdprConsent(true)
                .photoConsent(true)
                .build();
        registrationRepository.save(registration);
    }

    @Test
    @WithMockUser(username = "john.doe@msg.group", authorities = "PARTICIPANT")
    void getRegistrationsCount_returnsCount_forAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/registration/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));
    }

    @Test
    @WithMockUser(username = "jane.smith@msg.group", authorities = "MARKETING_ORGANIZER")
    void getRegistrationsCount_returnsZero_forUserWithoutRegistrations() throws Exception {
        mockMvc.perform(get("/api/registration/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    @Test
    void getRegistrationsCount_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/registration/count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "jane.smith@msg.group", authorities = "HR_USER")
    void previewReport_returnsAttendanceRows_forExistingEvent() throws Exception {
        mockMvc.perform(get("/api/registration/api/events/" + event.getId() + "/attendance-report/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].email").value("john.doe@msg.group"))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].lastName").value("Doe"))
                .andExpect(jsonPath("$[0].hasGdprConsent").value(true));
    }

    @Test
    @WithMockUser(username = "jane.smith@msg.group", authorities = "HR_USER")
    void previewReport_returnsEmptyArray_forEventWithoutRegistrations() throws Exception {
        Event empty = Event.builder()
                .name("No Registrations")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.PUBLISHED)
                .eventStartDate(OffsetDateTime.now().plusDays(4))
                .eventEndTime(OffsetDateTime.now().plusDays(4).plusHours(2))
                .registrationEndDate(OffsetDateTime.now().plusDays(3))
                .createdBy(organizer)
                .build();
        eventRepository.save(empty);

        mockMvc.perform(get("/api/registration/api/events/" + empty.getId() + "/attendance-report/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "jane.smith@msg.group", authorities = "HR_USER")
    void previewReport_returnsEmptyArray_forNonExistingEvent() throws Exception {
        mockMvc.perform(get("/api/registration/api/events/" + UUID.randomUUID() + "/attendance-report/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void previewReport_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/registration/api/events/" + event.getId() + "/attendance-report/preview"))
                .andExpect(status().isUnauthorized());
    }
}

