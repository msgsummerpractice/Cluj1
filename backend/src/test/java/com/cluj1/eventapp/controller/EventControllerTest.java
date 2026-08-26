package com.cluj1.eventapp.controller;

import com.cluj1.eventapp.config.SecurityConfig;
import com.cluj1.eventapp.dto.AttendanceRecordDto;
import com.cluj1.eventapp.dto.CheckInCodesDto;
import com.cluj1.eventapp.dto.EventDetailsDto;
import com.cluj1.eventapp.dto.EventDto;
import com.cluj1.eventapp.dto.EventRegistrationDto;
import com.cluj1.eventapp.dto.EventStatisticsDto;
import com.cluj1.eventapp.exception.GlobalExceptionHandler;
import com.cluj1.eventapp.exception.InvalidEventOperationException;
import com.cluj1.eventapp.mapper.EventMapper;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.security.JwtAuthenticationFilter;
import com.cluj1.eventapp.service.AttendanceExcelGeneratorService;
import com.cluj1.eventapp.service.EventCheckInService;
import com.cluj1.eventapp.service.EventDetailsService;
import com.cluj1.eventapp.service.EventExportService;
import com.cluj1.eventapp.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        SecurityConfig.class, JwtAuthenticationFilter.class }))
@Import({ EventControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class })
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

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @WithMockUser(username = "user@msg.group", authorities = "PARTICIPANT")
    void getRegistrationCountPerUser_returnsCount() throws Exception {
        when(eventService.getUpcomingRegisteredEventsCountPerUserByEmail("user@msg.group")).thenReturn(4);

        mockMvc.perform(get("/api/events/countRegistrationPerUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(4));
    }

    @Test
    void getEventById_returns200_forAuthenticatedParticipant() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventService.getEventById(id)).thenReturn(EventDto.builder().id(id).name("E").build());

        mockMvc.perform(get("/api/events/" + id)
                .with(user("u").authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("E"));
    }

    @Test
    void getEventById_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/events/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEventDetails_returns200_forAnyAuthorizedRole() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventDetailsService.getEventDetailsByEventId(id))
                .thenReturn(EventDetailsDto.builder().id(UUID.randomUUID()).eventId(id).description("desc").build());

        mockMvc.perform(get("/api/events/" + id + "/details")
                .with(user("u").authorities(new SimpleGrantedAuthority("HR_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("desc"));
    }

    @Test
    void getEventCheckInDetails_returns200_forParticipant() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventService.getCheckInDetails(id))
                .thenReturn(new CheckInCodesDto("data:image/png;base64,X", "ABC123"));

        mockMvc.perform(get("/api/events/" + id + "/checkin")
                .with(user("u").authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("ABC123"));
    }

    @Test
    void createEvent_returns201_forMarketingOrganizer() throws Exception {
        String eventJson = """
                {"name":"New Event","type":"LOCAL","location":"CLUJ","foodProvided":true,
                 "startDate":"2027-01-01T10:00:00+00:00","endDate":"2027-01-01T18:00:00+00:00"}
                """;
        EventDto response = EventDto.builder().id(UUID.randomUUID()).name("New Event")
                .status(EventStatus.DRAFT).build();
        when(eventService.createEvent(any(EventDto.class), any())).thenReturn(response);

        MockMultipartFile eventPart = new MockMultipartFile("event", "",
                MediaType.APPLICATION_JSON_VALUE, eventJson.getBytes());
        MockMultipartFile posterPart = new MockMultipartFile("poster", "p.png",
                MediaType.IMAGE_PNG_VALUE, new byte[] { 1, 2 });

        mockMvc.perform(multipart("/api/events")
                .file(eventPart).file(posterPart)
                .with(user("organizer").authorities(new SimpleGrantedAuthority("MARKETING_ORGANIZER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createEvent_returns403_forParticipant() throws Exception {
        String eventJson = """
                {"name":"X","type":"LOCAL","location":"CLUJ",
                 "startDate":"2027-01-01T10:00:00+00:00","endDate":"2027-01-01T18:00:00+00:00"}
                """;
        MockMultipartFile eventPart = new MockMultipartFile("event", "",
                MediaType.APPLICATION_JSON_VALUE, eventJson.getBytes());

        mockMvc.perform(multipart("/api/events").file(eventPart)
                .with(user("p").authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateEvent_returns200_forMarketingOrganizer() throws Exception {
        UUID id = UUID.randomUUID();
        String eventJson = """
                {"name":"Updated","type":"LOCAL","location":"CLUJ","foodProvided":true,
                 "startDate":"2027-01-01T10:00:00+00:00","endDate":"2027-01-01T18:00:00+00:00"}
                """;
        EventDto response = EventDto.builder().id(id).name("Updated").build();
        when(eventService.updateEvent(eq(id), any(EventDto.class), any())).thenReturn(response);

        MockMultipartFile eventPart = new MockMultipartFile("event", "",
                MediaType.APPLICATION_JSON_VALUE, eventJson.getBytes());

        var builder = multipart("/api/events/" + id).file(eventPart)
                .with(user("organizer").authorities(new SimpleGrantedAuthority("MARKETING_ORGANIZER")));
        builder.with(req -> {
            req.setMethod("PUT");
            return req;
        });

        mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void getEventPoster_returnsPngContentType_whenPngBytes() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] png = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
        when(eventDetailsService.getPosterByEventId(id)).thenReturn(Optional.of(png));

        mockMvc.perform(get("/api/events/" + id + "/poster")
                .with(user("u").authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void getEventPoster_returnsJpegContentType_whenJpegBytes() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] jpeg = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0 };
        when(eventDetailsService.getPosterByEventId(id)).thenReturn(Optional.of(jpeg));

        mockMvc.perform(get("/api/events/" + id + "/poster")
                .with(user("u").authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void getEventPoster_returnsWebpContentType_whenRiffWebpBytes() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] webp = { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P' };
        when(eventDetailsService.getPosterByEventId(id)).thenReturn(Optional.of(webp));

        mockMvc.perform(get("/api/events/" + id + "/poster")
                .with(user("u").authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.valueOf("image/webp")));
    }

    @Test
    void getEventPoster_returnsGifContentType_whenGifBytes() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] gif = { 0x47, 'I', 'F', '8' };
        when(eventDetailsService.getPosterByEventId(id)).thenReturn(Optional.of(gif));

        mockMvc.perform(get("/api/events/" + id + "/poster")
                .with(user("u").authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_GIF));
    }

    @Test
    void getEventPoster_returns404_whenPosterMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventDetailsService.getPosterByEventId(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/events/" + id + "/poster")
                .with(user("u").authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadAttendanceReport_returns200_forHrUser_whenEventCompleted() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventService.getEventById(id)).thenReturn(EventDto.builder().status(EventStatus.COMPLETED).build());
        when(registrationRepository.findAttendanceReportRows(id)).thenReturn(List.of());
        when(attendanceReportExcelGenerator.generate(any())).thenReturn(new byte[] { 1, 2 });

        mockMvc.perform(get("/api/events/" + id + "/attendance-report")
                .with(user("hr").authorities(new SimpleGrantedAuthority("HR_USER"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=attendance-report-" + id + ".xlsx"));
    }

    @Test
    void downloadAttendanceReport_returns409_whenEventNotCompleted() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventService.getEventById(id)).thenReturn(EventDto.builder().status(EventStatus.PUBLISHED).build());

        mockMvc.perform(get("/api/events/" + id + "/attendance-report")
                .with(user("hr").authorities(new SimpleGrantedAuthority("HR_USER"))))
                .andExpect(status().isConflict());
    }

    @Test
    void downloadAttendanceReport_returns403_forMarketingOrganizer() throws Exception {
        mockMvc.perform(get("/api/events/" + UUID.randomUUID() + "/attendance-report")
                .with(user("m").authorities(new SimpleGrantedAuthority("MARKETING_ORGANIZER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user@msg.group", authorities = "PARTICIPANT")
    void registerForEvent_returns200_withSuccessMessage() throws Exception {
        UUID id = UUID.randomUUID();
        EventRegistrationDto dto = EventRegistrationDto.builder().gdprConsent(true).photoConsent(true).build();

        mockMvc.perform(post("/api/events/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully registered for the event."));

        verify(eventService).registerUser(eq(id), eq("user@msg.group"), any(EventRegistrationDto.class));
    }

    @Test
    @WithMockUser(username = "user@msg.group", authorities = "PARTICIPANT")
    void registerForEvent_returns400_whenServiceThrowsInvalidEventOperation() throws Exception {
        UUID id = UUID.randomUUID();
        EventRegistrationDto dto = EventRegistrationDto.builder().gdprConsent(true).photoConsent(true).build();
        doThrow(new InvalidEventOperationException("closed")).when(eventService)
                .registerUser(eq(id), any(), any());

        mockMvc.perform(post("/api/events/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@msg.group")
    void checkIfRegistered_returnsBoolean() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventService.isUserRegistered(id, "user@msg.group")).thenReturn(true);

        mockMvc.perform(get("/api/events/" + id + "/check"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void getEventStatistics_returns200_forMarketingOrganizer() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventService.getEventStatistics(id)).thenReturn(EventStatisticsDto.builder()
                .invitedCount(100).registrationCount(50).participantCount(30).build());

        mockMvc.perform(get("/api/events/" + id + "/statistics")
                .with(user("m").authorities(new SimpleGrantedAuthority("MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitedCount").value(100))
                .andExpect(jsonPath("$.registrationCount").value(50));
    }

    @Test
    void getEventStatistics_returns403_forParticipant() throws Exception {
        mockMvc.perform(get("/api/events/" + UUID.randomUUID() + "/statistics")
                .with(user("p").authorities(new SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRecentCheckins_returnsList_forHrUser() throws Exception {
        UUID id = UUID.randomUUID();
        AttendanceRecordDto record = AttendanceRecordDto.builder().id(UUID.randomUUID())
                .checkInTime(OffsetDateTime.now()).build();
        when(eventCheckInService.getRecentCheckins(eq(id), anyInt())).thenReturn(List.of(record));

        mockMvc.perform(get("/api/events/" + id + "/checkins/recent")
                .with(user("hr").authorities(new SimpleGrantedAuthority("HR_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updateEventStatus_returns200_forMarketingOrganizer() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventService.updateEventStatus(id, EventStatus.PUBLISHED))
                .thenReturn(EventDto.builder().id(id).status(EventStatus.PUBLISHED).build());

        mockMvc.perform(patch("/api/events/" + id + "/status/PUBLISHED")
                .with(user("m").authorities(new SimpleGrantedAuthority("MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void generateCheckInCodes_returnsCodes_forMarketingOrganizer() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventService.generateCheckInCodes(id)).thenReturn(new CheckInCodesDto("qr", "ABC123"));

        mockMvc.perform(post("/api/events/" + id + "/checkin-codes")
                .with(user("m").authorities(new SimpleGrantedAuthority("MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("ABC123"));
    }

    @Test
    @WithMockUser(username = "user@msg.group", authorities = "PARTICIPANT")
    void updateRegistration_returnsSuccessMessage() throws Exception {
        UUID id = UUID.randomUUID();
        Registration reg = Registration.builder().id(UUID.randomUUID()).build();
        when(eventService.updateRegistration(eq(id), eq("user@msg.group"), any())).thenReturn(reg);

        mockMvc.perform(patch("/api/events/" + id + "/manage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(
                        EventRegistrationDto.builder().gdprConsent(true).photoConsent(true).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully updated registration"));
    }

    @Test
    @WithMockUser(username = "user@msg.group", authorities = "PARTICIPANT")
    void updateRegistration_returnsGdprRemovalMessage_whenServiceReturnsNull() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventService.updateRegistration(eq(id), eq("user@msg.group"), any())).thenReturn(null);

        mockMvc.perform(patch("/api/events/" + id + "/manage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(
                        EventRegistrationDto.builder().gdprConsent(false).photoConsent(true).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration automatically removed due to GDPR consent"));
    }

    @Test
    @WithMockUser(username = "user@msg.group", authorities = "PARTICIPANT")
    void deleteRegistration_returns200_onSuccess() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/events/" + id + "/manage"))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully deleted registration"));

        verify(eventService).deleteRegistration(id, "user@msg.group");
    }

    @Test
    @WithMockUser(username = "user@msg.group", authorities = "PARTICIPANT")
    void deleteRegistration_returns500_whenServiceThrows() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("boom")).when(eventService).deleteRegistration(id, "user@msg.group");

        mockMvc.perform(delete("/api/events/" + id + "/manage"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error deleting registration"));
    }

    @Test
    @WithMockUser(username = "user@msg.group", authorities = "PARTICIPANT")
    void getRegistrationDetails_returnsMappedDto() throws Exception {
        UUID id = UUID.randomUUID();
        Registration reg = Registration.builder().id(UUID.randomUUID()).build();
        when(eventService.getRegistration(id, "user@msg.group")).thenReturn(reg);
        when(eventMapper.toEventRegistrationDto(reg)).thenReturn(
                EventRegistrationDto.builder().gdprConsent(true).photoConsent(false).build());

        mockMvc.perform(get("/api/events/" + id + "/registration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gdprConsent").value(true))
                .andExpect(jsonPath("$.photoConsent").value(false));
    }
}