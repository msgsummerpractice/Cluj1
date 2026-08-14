package com.cluj1.eventapp.controller;

import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class EventIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

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

        eventRepository.deleteAll();
        userRepository.deleteAll();

        savedUser = userRepository.save(User.builder()
                .email("organizer.test@msg.group")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.MARKETING_ORGANIZER)
                .isActive(true)
                .build());
    }

    private Event buildEvent(String name) {
        return Event.builder()
                .name(name)
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.DRAFT)
                .createdBy(savedUser)
                .build();
    }

    @Test
    void getAllEventsReturnEmptyArrayWhenNoEventsExist() throws Exception {
        mockMvc.perform(get("/api/events")
                .with(user("marketingUser").roles("MARKETING_ORGANIZER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllEventsReturnAllSavedEvents() throws Exception {
        eventRepository.save(buildEvent("Event 1"));
        eventRepository.save(buildEvent("Event 2"));
        eventRepository.save(buildEvent("Event 3"));

        mockMvc.perform(get("/api/events")
                .with(user("marketingUser").roles("MARKETING_ORGANIZER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void getAllEventsMapAllDtoFieldsCorrectly() throws Exception {
        eventRepository.save(buildEvent("Summer Fest"));

        mockMvc.perform(get("/api/events")
                .with(user("marketingUser").roles("MARKETING_ORGANIZER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].name").value("Summer Fest"))
                .andExpect(jsonPath("$[0].location").value("CLUJ"))
                .andExpect(jsonPath("$[0].type").value("LOCAL"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    void getAllEventsReturnNullDatesWhenNotSet() throws Exception {
        eventRepository.save(buildEvent("No Date Event"));

        mockMvc.perform(get("/api/events")
                .with(user("marketingUser").roles("MARKETING_ORGANIZER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startDate").doesNotExist())
                .andExpect(jsonPath("$[0].endDate").doesNotExist());
    }

    @Test
    void getAllEventsReturnDatesWhenSet() throws Exception {
        OffsetDateTime start = OffsetDateTime.parse("2026-09-01T10:00:00+00:00");
        OffsetDateTime end = OffsetDateTime.parse("2026-09-01T18:00:00+00:00");

        eventRepository.save(Event.builder()
                .name("Dated Event")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.PUBLISHED)
                .eventStartDate(start)
                .eventEndTime(end)
                .createdBy(savedUser)
                .build());

        mockMvc.perform(get("/api/events")
                .with(user("marketingUser").roles("MARKETING_ORGANIZER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startDate").isNotEmpty())
                .andExpect(jsonPath("$[0].endDate").isNotEmpty());
    }

    @Test
    void getAllEventsReflectDifferentEventStatuses() throws Exception {
        eventRepository.save(buildEvent("Draft Event"));
        eventRepository.save(Event.builder()
                .name("Published Event")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.PUBLISHED)
                .createdBy(savedUser)
                .build());

        mockMvc.perform(get("/api/events")
                .with(user("marketingUser").roles("MARKETING_ORGANIZER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllEventsReflectDifferentLocationsAndTypes() throws Exception {
        eventRepository.save(Event.builder()
                .name("External Event")
                .location(EventLocation.TIMISOARA)
                .type(EventType.EXTERNAL)
                .status(EventStatus.DRAFT)
                .createdBy(savedUser)
                .build());

        mockMvc.perform(get("/api/events")
                .with(user("hrUser").roles("HR_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].location").value("TIMISOARA"))
                .andExpect(jsonPath("$[0].type").value("EXTERNAL"));
    }

    @Test
    @WithMockUser(username = "organizer.test@msg.group", roles = "MARKETING_ORGANIZER")
    void shouldCreateEventSuccessfully() throws Exception {
        String eventJson = """
                    {
                        "name": "Integration Test Event",
                        "type": "LOCAL",
                        "location": "TIMISOARA",
                        "foodProvided": true
                    }
                """;

        MockMultipartFile eventPart = new MockMultipartFile("event", "", MediaType.APPLICATION_JSON_VALUE,
                eventJson.getBytes());
        MockMultipartFile posterPart = new MockMultipartFile("poster", "poster.png", "image/png",
                "dummy-image-data".getBytes());

        mockMvc.perform(multipart("/api/events")
                .file(eventPart)
                .file(posterPart)
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Integration Test Event"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }
}