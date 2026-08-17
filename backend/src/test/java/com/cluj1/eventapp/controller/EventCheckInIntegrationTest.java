package com.cluj1.eventapp.controller;

import com.cluj1.eventapp.dto.CheckInRequest;
import com.cluj1.eventapp.model.AttendanceRecord;
import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.EventDetails;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.CheckInMethod;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.repository.AttendanceRecordRepository;
import com.cluj1.eventapp.repository.EventDetailsRepository;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class EventCheckInIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventDetailsRepository eventDetailsRepository;
    @Autowired
    private RegistrationRepository registrationRepository;
    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User participant;
    private Event event;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        attendanceRecordRepository.deleteAll();
        registrationRepository.deleteAll();
        eventDetailsRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        participant = userRepository.save(User.builder()
                .email("john.doe@msg.group")
                .passwordHash(passwordEncoder.encode("Password1!"))
                .role(Role.PARTICIPANT)
                .isActive(true)
                .build());

        User organizer = userRepository.save(User.builder()
                .email("organizer.test@msg.group")
                .passwordHash(passwordEncoder.encode("Password1!"))
                .role(Role.MARKETING_ORGANIZER)
                .isActive(true)
                .build());

        event = eventRepository.save(Event.builder()
                .name("Tech Summit 2026")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.PUBLISHED)
                .createdBy(organizer)
                .build());
    }

    private Registration registerParticipant() {
        return registrationRepository.save(Registration.builder()
                .user(participant)
                .event(event)
                .build());
    }

    private String json(CheckInRequest req) throws Exception {
        return objectMapper.writeValueAsString(req);
    }

    private CheckInRequest request(String code, CheckInMethod method) {
        CheckInRequest req = new CheckInRequest();
        req.setCode(code);
        req.setMethod(method);
        return req;
    }

    @Test
    @WithMockUser(username = "john.doe@msg.group", roles = "PARTICIPANT")
    void checkIn_withEventUuid_returns200AndPersistsRecord() throws Exception {
        Registration reg = registerParticipant();

        mockMvc.perform(post("/api/events/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request(event.getId().toString(), CheckInMethod.QR))))
                .andExpect(status().isOk());

        assertThat(attendanceRecordRepository.existsByRegistrationId(reg.getId())).isTrue();
        AttendanceRecord record = attendanceRecordRepository.findAll().get(0);
        assertThat(record.getCheckInMethod()).isEqualTo(CheckInMethod.QR);
    }

    @Test
    @WithMockUser(username = "john.doe@msg.group", roles = "PARTICIPANT")
    void checkIn_withEventCode_returns200AndPersistsRecord() throws Exception {
        eventDetailsRepository.save(EventDetails.builder()
                .event(event)
                .foodProvided(false)
                .eventCode("TK2026")
                .build());
        Registration reg = registerParticipant();

        mockMvc.perform(post("/api/events/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request("TK2026", CheckInMethod.MANUAL))))
                .andExpect(status().isOk());

        assertThat(attendanceRecordRepository.existsByRegistrationId(reg.getId())).isTrue();
        AttendanceRecord record = attendanceRecordRepository.findAll().get(0);
        assertThat(record.getCheckInMethod()).isEqualTo(CheckInMethod.MANUAL);
    }

    @Test
    void checkIn_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/events/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request(event.getId().toString(), CheckInMethod.QR))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "john.doe@msg.group", roles = "PARTICIPANT")
    void checkIn_userNotRegistered_returns400() throws Exception {
        mockMvc.perform(post("/api/events/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request(event.getId().toString(), CheckInMethod.QR))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("checkin.error.user.notregistered"));
    }

    @Test
    @WithMockUser(username = "john.doe@msg.group", roles = "PARTICIPANT")
    void checkIn_duplicateCheckIn_returns400() throws Exception {
        Registration reg = registerParticipant();
        attendanceRecordRepository.save(AttendanceRecord.builder()
                .registration(reg)
                .checkInMethod(CheckInMethod.QR)
                .build());

        mockMvc.perform(post("/api/events/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request(event.getId().toString(), CheckInMethod.QR))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("checkin.error.user.alreadycheckedin"));
    }

    @Test
    @WithMockUser(username = "john.doe@msg.group", roles = "PARTICIPANT")
    void checkIn_eventCompleted_returns400() throws Exception {
        event.setStatus(EventStatus.COMPLETED);
        eventRepository.save(event);
        registerParticipant();

        mockMvc.perform(post("/api/events/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request(event.getId().toString(), CheckInMethod.QR))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("checkin.error.event.completed"));
    }

    @Test
    @WithMockUser(username = "john.doe@msg.group", roles = "PARTICIPANT")
    void checkIn_eventEndTimeExpired_returns400() throws Exception {
        event.setEventEndTime(OffsetDateTime.now().minusHours(1));
        eventRepository.save(event);
        registerParticipant();

        mockMvc.perform(post("/api/events/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request(event.getId().toString(), CheckInMethod.QR))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("checkin.error.event.expired"));
    }

    @Test
    @WithMockUser(username = "john.doe@msg.group", roles = "PARTICIPANT")
    void checkIn_unknownEventCode_returns404() throws Exception {
        mockMvc.perform(post("/api/events/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request("XXXXXX", CheckInMethod.MANUAL))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("checkin.error.event.notfound"));
    }

    @Test
    @WithMockUser(username = "john.doe@msg.group", roles = "PARTICIPANT")
    void checkIn_unknownEventUuid_returns404() throws Exception {
        mockMvc.perform(post("/api/events/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request(java.util.UUID.randomUUID().toString(), CheckInMethod.QR))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("checkin.error.event.notfound"));
    }

    @Test
    @WithMockUser(username = "john.doe@msg.group", roles = "PARTICIPANT")
    void checkIn_codeTooShort_returns400() throws Exception {
        mockMvc.perform(post("/api/events/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request("AB1", CheckInMethod.MANUAL))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "john.doe@msg.group", roles = "PARTICIPANT")
    void checkIn_nullMethod_returns400() throws Exception {
        CheckInRequest req = new CheckInRequest();
        req.setCode("ABC123");
        req.setMethod(null);

        mockMvc.perform(post("/api/events/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
