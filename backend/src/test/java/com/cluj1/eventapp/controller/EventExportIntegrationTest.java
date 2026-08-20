package com.cluj1.eventapp.controller;

import com.cluj1.eventapp.model.*;
import com.cluj1.eventapp.model.enums.*;
import com.cluj1.eventapp.repository.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class EventExportIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserDetailsRepository userDetailsRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private RegistrationRepository registrationRepository;
    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User organizer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        attendanceRecordRepository.deleteAll();
        registrationRepository.deleteAll();
        eventRepository.deleteAll();
        userDetailsRepository.deleteAll();
        userRepository.deleteAll();

        organizer = userRepository.save(User.builder()
                .email("organizer.export@msg.group")
                .passwordHash(passwordEncoder.encode("Password1!"))
                .role(Role.MARKETING_ORGANIZER)
                .isActive(true)
                .build());
    }

    private Event saveEvent(String name, EventStatus status) {
        return eventRepository.save(Event.builder()
                .name(name)
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(status)
                .createdBy(organizer)
                .build());
    }

    private User saveParticipant(String email, String firstName, String lastName) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1!"))
                .role(Role.PARTICIPANT)
                .isActive(true)
                .build());
        com.cluj1.eventapp.model.UserDetails ud = userDetailsRepository.save(
                com.cluj1.eventapp.model.UserDetails.builder()
                        .user(user)
                        .firstName(firstName)
                        .lastName(lastName)
                        .location(UserLocation.CLUJ)
                        .build());
        user.setUserDetails(ud);
        return user;
    }

    private Registration saveRegistration(User user, Event event) {
        return registrationRepository.save(Registration.builder()
                .user(user)
                .event(event)
                .gdprConsent(true)
                .transportationNeeded(false)
                .accommodationNeeded(false)
                .build());
    }

    private Sheet parseFirstSheet(byte[] bytes) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes)).getSheetAt(0);
    }

    private String cellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null)
            return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default -> "";
        };
    }

    private static final String EXPORT_URL = "/api/events/{id}/export";

    @Test
    void exportReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
        Event event = saveEvent("Test Event", EventStatus.PUBLISHED);

        mockMvc.perform(get(EXPORT_URL, event.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportReturnsForbiddenForParticipant() throws Exception {
        Event event = saveEvent("Test Event", EventStatus.PUBLISHED);
        User participant = saveParticipant("p.user@msg.group", "P", "User");

        mockMvc.perform(get(EXPORT_URL, event.getId())
                .with(user(participant.getEmail())
                        .authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("PARTICIPANT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportReturnsBadRequestForDraftEvent() throws Exception {
        Event draftEvent = saveEvent("Draft Event", EventStatus.DRAFT);

        mockMvc.perform(get(EXPORT_URL, draftEvent.getId())
                .with(user(organizer.getEmail())
                        .authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "MARKETING_ORGANIZER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportReturnsOkForPublishedEvent() throws Exception {
        Event event = saveEvent("Published Event", EventStatus.PUBLISHED);

        mockMvc.perform(get(EXPORT_URL, event.getId())
                .with(user(organizer.getEmail())
                        .authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"attendance_report_" + event.getId() + ".xlsx\""));
    }

    @Test
    void exportReturnsOkForCompletedEvent() throws Exception {
        Event event = saveEvent("Completed Event", EventStatus.COMPLETED);

        mockMvc.perform(get(EXPORT_URL, event.getId())
                .with(user(organizer.getEmail())
                        .authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "MARKETING_ORGANIZER"))))
                .andExpect(status().isOk());
    }

    @Test
    void exportReturnsBadRequestWhenEventDoesNotExist() throws Exception {
        // GlobalExceptionHandler maps IllegalArgumentException → 400 Bad Request
        mockMvc.perform(get(EXPORT_URL, UUID.randomUUID())
                .with(user(organizer.getEmail())
                        .authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "MARKETING_ORGANIZER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportProducesValidExcelWithHeaderRowWhenNoRegistrations() throws Exception {
        Event event = saveEvent("Empty Event", EventStatus.PUBLISHED);

        MvcResult result = mockMvc.perform(get(EXPORT_URL, event.getId())
                .with(user(organizer.getEmail())
                        .authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andReturn();

        Sheet sheet = parseFirstSheet(result.getResponse().getContentAsByteArray());
        Row header = sheet.getRow(0);

        assertThat(header.getCell(0).getStringCellValue()).isEqualTo("nr_crt");
        assertThat(header.getCell(1).getStringCellValue()).isEqualTo("lastName");
        assertThat(header.getCell(2).getStringCellValue()).isEqualTo("firstName");
        assertThat(header.getCell(3).getStringCellValue()).isEqualTo("eventName");
        assertThat(header.getCell(4).getStringCellValue()).isEqualTo("email");
        assertThat(header.getCell(5).getStringCellValue()).isEqualTo("foodPreference");
        assertThat(header.getCell(6).getStringCellValue()).isEqualTo("transportRequiered");
        assertThat(header.getCell(7).getStringCellValue()).isEqualTo("accomodationRequired");
        assertThat(header.getCell(8).getStringCellValue()).isEqualTo("driverName");
        assertThat(header.getCell(9).getStringCellValue()).isEqualTo("driverPhoneNumber");
        assertThat(header.getCell(10).getStringCellValue()).isEqualTo("gdpr");
        assertThat(sheet.getLastRowNum()).isEqualTo(0); // only header, no data rows
    }

    @Test
    void exportIncludesParticipantNameEmailAndEventName() throws Exception {
        Event event = saveEvent("Tech Summit", EventStatus.PUBLISHED);
        User participant = saveParticipant("alice.brown@msg.group", "Alice", "Brown");
        saveRegistration(participant, event);

        MvcResult result = mockMvc.perform(get(EXPORT_URL, event.getId())
                .with(user(organizer.getEmail())
                        .authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andReturn();

        Sheet sheet = parseFirstSheet(result.getResponse().getContentAsByteArray());
        Row row = sheet.getRow(1);

        assertThat(cellString(row, 0)).isEqualTo("1");
        assertThat(cellString(row, 1)).isEqualTo("Brown");
        assertThat(cellString(row, 2)).isEqualTo("Alice");
        assertThat(cellString(row, 3)).isEqualTo("Tech Summit");
        assertThat(cellString(row, 4)).isEqualTo("alice.brown@msg.group");
    }

    @Test
    void exportSetsEmptyDriverFieldsForParticipantWithNoTransport() throws Exception {
        Event event = saveEvent("Tech Summit", EventStatus.PUBLISHED);
        User participant = saveParticipant("bob.smith@msg.group", "Bob", "Smith");
        registrationRepository.save(Registration.builder()
                .user(participant).event(event)
                .gdprConsent(true).transportationNeeded(false).accommodationNeeded(false)
                .build());

        MvcResult result = mockMvc.perform(get(EXPORT_URL, event.getId())
                .with(user(organizer.getEmail())
                        .authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andReturn();

        Sheet sheet = parseFirstSheet(result.getResponse().getContentAsByteArray());
        Row row = sheet.getRow(1);

        assertThat(cellString(row, 6)).isEqualTo("no");
        assertThat(cellString(row, 8)).isEmpty();
        assertThat(cellString(row, 9)).isEmpty();
    }

    @Test
    void exportAssignsSequentialNrCrtForMultipleParticipants() throws Exception {
        Event event = saveEvent("Tech Summit", EventStatus.PUBLISHED);
        User u1 = saveParticipant("alice.brown@msg.group", "Alice", "Brown");
        User u2 = saveParticipant("charlie.davis@msg.group", "Charlie", "Davis");
        User u3 = saveParticipant("eve.ford@msg.group", "Eve", "Ford");
        saveRegistration(u1, event);
        saveRegistration(u2, event);
        saveRegistration(u3, event);

        MvcResult result = mockMvc.perform(get(EXPORT_URL, event.getId())
                .with(user(organizer.getEmail())
                        .authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andReturn();

        Sheet sheet = parseFirstSheet(result.getResponse().getContentAsByteArray());

        assertThat(sheet.getLastRowNum()).isEqualTo(3); // header + 3 data rows
        assertThat(cellString(sheet.getRow(1), 0)).isEqualTo("1");
        assertThat(cellString(sheet.getRow(2), 0)).isEqualTo("2");
        assertThat(cellString(sheet.getRow(3), 0)).isEqualTo("3");
    }

    @Test
    void exportSetsGdprConsentCorrectly() throws Exception {
        Event event = saveEvent("Tech Summit", EventStatus.PUBLISHED);
        User u1 = saveParticipant("alice.brown@msg.group", "Alice", "Brown");
        User u2 = saveParticipant("bob.smith@msg.group", "Bob", "Smith");
        registrationRepository.save(Registration.builder()
                .user(u1).event(event).gdprConsent(true)
                .transportationNeeded(false).accommodationNeeded(false).build());
        registrationRepository.save(Registration.builder()
                .user(u2).event(event).gdprConsent(false)
                .transportationNeeded(false).accommodationNeeded(false).build());

        MvcResult result = mockMvc.perform(get(EXPORT_URL, event.getId())
                .with(user(organizer.getEmail())
                        .authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andReturn();

        Sheet sheet = parseFirstSheet(result.getResponse().getContentAsByteArray());
        assertThat(cellString(sheet.getRow(1), 10)).isEqualTo("yes");
        assertThat(cellString(sheet.getRow(2), 10)).isEqualTo("no");
    }

    @Test
    void exportSetsFoodPreferenceCorrectly() throws Exception {
        Event event = saveEvent("Tech Summit", EventStatus.PUBLISHED);
        User u1 = saveParticipant("alice.brown@msg.group", "Alice", "Brown");
        User u2 = saveParticipant("charlie.davis@msg.group", "Charlie", "Davis");
        User u3 = saveParticipant("eve.ford@msg.group", "Eve", "Ford");
        registrationRepository.save(Registration.builder()
                .user(u1).event(event).gdprConsent(true)
                .foodPreference(FoodPreference.NONE)
                .transportationNeeded(false).accommodationNeeded(false).build());
        registrationRepository.save(Registration.builder()
                .user(u2).event(event).gdprConsent(true)
                .foodPreference(FoodPreference.VEGETARIAN)
                .transportationNeeded(false).accommodationNeeded(false).build());
        registrationRepository.save(Registration.builder()
                .user(u3).event(event).gdprConsent(true)
                .foodPreference(FoodPreference.VEGAN)
                .transportationNeeded(false).accommodationNeeded(false).build());

        MvcResult result = mockMvc.perform(get(EXPORT_URL, event.getId())
                .with(user(organizer.getEmail())
                        .authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andReturn();

        Sheet sheet = parseFirstSheet(result.getResponse().getContentAsByteArray());
        assertThat(cellString(sheet.getRow(1), 5)).isEmpty(); // NONE → blank
        assertThat(cellString(sheet.getRow(2), 5)).isEqualTo("VEGETARIAN");
        assertThat(cellString(sheet.getRow(3), 5)).isEqualTo("VEGAN");
    }

    @Test
    void exportSetsAccommodationWithDayCountWhenRequired() throws Exception {
        Event event = saveEvent("Tech Summit", EventStatus.PUBLISHED);
        User participant = saveParticipant("alice.brown@msg.group", "Alice", "Brown");
        registrationRepository.save(Registration.builder()
                .user(participant).event(event).gdprConsent(true)
                .transportationNeeded(false).accommodationNeeded(true).accommodationDays(2)
                .build());

        MvcResult result = mockMvc.perform(get(EXPORT_URL, event.getId())
                .with(user(organizer.getEmail())
                        .authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "MARKETING_ORGANIZER"))))
                .andExpect(status().isOk())
                .andReturn();

        Sheet sheet = parseFirstSheet(result.getResponse().getContentAsByteArray());
        assertThat(cellString(sheet.getRow(1), 7)).isEqualTo("yes (2 days)");
    }
}
