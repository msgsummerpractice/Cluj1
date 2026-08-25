package com.cluj1.eventapp.controller;

import com.cluj1.eventapp.model.AttendanceRecord;
import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.UserDetails;
import com.cluj1.eventapp.model.enums.CheckInMethod;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.model.enums.UserLocation;
import com.cluj1.eventapp.repository.AttendanceRecordRepository;
import com.cluj1.eventapp.repository.EventDetailsRepository;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.repository.UserDetailsRepository;
import com.cluj1.eventapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class EligibleEventsIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventDetailsRepository eventDetailsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PARTICIPANT_EMAIL = "eligible.participant@msg.group";

    private User organizer;
    private User participant;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        attendanceRecordRepository.deleteAll();
        registrationRepository.deleteAll();
        eventDetailsRepository.deleteAll();
        eventRepository.deleteAll();
        userDetailsRepository.deleteAll();
        userRepository.deleteAll();

        organizer = userRepository.save(User.builder()
                .email("eligible.organizer@msg.group")
                .passwordHash(passwordEncoder.encode("Password1!"))
                .role(Role.MARKETING_ORGANIZER)
                .isActive(true)
                .build());

        participant = createParticipantWithLocation(UserLocation.CLUJ);
    }

    private User createParticipantWithLocation(UserLocation location) {
        User user = User.builder()
                .email(PARTICIPANT_EMAIL)
                .passwordHash(passwordEncoder.encode("Password1!"))
                .role(Role.PARTICIPANT)
                .isActive(true)
                .build();
        UserDetails details = UserDetails.builder()
                .user(user)
                .firstName("Test")
                .lastName("Participant")
                .location(location)
                .build();
        user.setUserDetails(details);
        return userRepository.save(user);
    }

    private Event savePublishedEvent(String name, EventLocation location) {
        return eventRepository.save(Event.builder()
                .name(name)
                .location(location)
                .type(EventType.LOCAL)
                .status(EventStatus.PUBLISHED)
                .registrationEndDate(OffsetDateTime.now().plusDays(30))
                .createdBy(organizer)
                .build());
    }

    @Test
    void getEligibleEventsExcludesDraftEvents() throws Exception {
        eventRepository.save(Event.builder()
                .name("Draft Event")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.DRAFT)
                .registrationEndDate(OffsetDateTime.now().plusDays(30))
                .createdBy(organizer)
                .build());

        mockMvc.perform(get("/api/events/eligible")
                .with(user(PARTICIPANT_EMAIL).authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getEligibleEventsExcludesCompletedEvents() throws Exception {
        eventRepository.save(Event.builder()
                .name("Completed Event")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.COMPLETED)
                .registrationEndDate(OffsetDateTime.now().plusDays(30))
                .createdBy(organizer)
                .build());

        mockMvc.perform(get("/api/events/eligible")
                .with(user(PARTICIPANT_EMAIL).authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getEligibleEventsExcludesEventsWithPastRegistrationEndDate() throws Exception {
        eventRepository.save(Event.builder()
                .name("Expired Event")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.PUBLISHED)
                .registrationEndDate(OffsetDateTime.now().minusDays(1))
                .createdBy(organizer)
                .build());

        mockMvc.perform(get("/api/events/eligible")
                .with(user(PARTICIPANT_EMAIL).authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getEligibleEventsReturnsEventMatchingUserLocation() throws Exception {
        savePublishedEvent("Cluj Event", EventLocation.CLUJ);

        mockMvc.perform(get("/api/events/eligible")
                .with(user(PARTICIPANT_EMAIL).authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Cluj Event"));
    }

    @Test
    void getEligibleEventsReturnsAllLocationEventForAnyUser() throws Exception {
        eventRepository.save(Event.builder()
                .name("Company All Hands")
                .location(EventLocation.ALL)
                .type(EventType.INTERNAL)
                .status(EventStatus.PUBLISHED)
                .registrationEndDate(OffsetDateTime.now().plusDays(30))
                .createdBy(organizer)
                .build());

        mockMvc.perform(get("/api/events/eligible")
                .with(user(PARTICIPANT_EMAIL).authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Company All Hands"));
    }

    @Test
    void getEligibleEventsExcludesEventWithNonMatchingLocation() throws Exception {
        savePublishedEvent("Timisoara Event", EventLocation.TIMISOARA);

        mockMvc.perform(get("/api/events/eligible")
                .with(user(PARTICIPANT_EMAIL).authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getEligibleEventsReturnsIsRegisteredFalseWhenNotRegistered() throws Exception {
        savePublishedEvent("Open Event", EventLocation.CLUJ);

        mockMvc.perform(get("/api/events/eligible")
                .with(user(PARTICIPANT_EMAIL).authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isRegistered").value(false))
                .andExpect(jsonPath("$[0].isCheckedIn").value(false));
    }

    @Test
    void getEligibleEventsReturnsIsRegisteredTrueForRegisteredParticipant() throws Exception {
        Event event = savePublishedEvent("Registered Event", EventLocation.CLUJ);
        registrationRepository.save(Registration.builder()
                .user(participant)
                .event(event)
                .build());

        mockMvc.perform(get("/api/events/eligible")
                .with(user(PARTICIPANT_EMAIL).authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isRegistered").value(true))
                .andExpect(jsonPath("$[0].isCheckedIn").value(false));
    }

    @Test
    void getEligibleEventsReturnsIsCheckedInTrueWhenAttendanceRecordExists() throws Exception {
        Event event = savePublishedEvent("CheckedIn Event", EventLocation.CLUJ);
        Registration registration = registrationRepository.save(Registration.builder()
                .user(participant)
                .event(event)
                .build());
        attendanceRecordRepository.save(AttendanceRecord.builder()
                .registration(registration)
                .checkInMethod(CheckInMethod.QR)
                .build());

        mockMvc.perform(get("/api/events/eligible")
                .with(user(PARTICIPANT_EMAIL).authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isRegistered").value(true))
                .andExpect(jsonPath("$[0].isCheckedIn").value(true));
    }

    @Test
    void getEligibleEventsReturns401ForUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/events/eligible"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEligibleEventsIncludesEventWithNoRegistrationEndDate() throws Exception {
        eventRepository.save(Event.builder()
                .name("Open Ended Event")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.PUBLISHED)
                .registrationEndDate(null)
                .createdBy(organizer)
                .build());

        mockMvc.perform(get("/api/events/eligible")
                .with(user(PARTICIPANT_EMAIL).authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Open Ended Event"));
    }
}
