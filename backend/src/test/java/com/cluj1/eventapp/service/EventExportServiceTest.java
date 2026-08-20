package com.cluj1.eventapp.service;

import com.cluj1.eventapp.exception.InvalidEventOperationException;
import com.cluj1.eventapp.model.*;
import com.cluj1.eventapp.model.enums.*;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.RegistrationRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventExportServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private EventExportService eventExportService;

    private User buildUser(String email, String firstName, String lastName) {
        com.cluj1.eventapp.model.UserDetails ud = com.cluj1.eventapp.model.UserDetails.builder()
                .firstName(firstName)
                .lastName(lastName)
                .location(UserLocation.CLUJ)
                .build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("hash")
                .role(Role.PARTICIPANT)
                .isActive(true)
                .build();
        ud.setUser(user);
        user.setUserDetails(ud);
        return user;
    }

    private Event buildEvent(EventStatus status) {
        return Event.builder()
                .id(UUID.randomUUID())
                .name("Tech Summit 2026")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(status)
                .build();
    }

    private Registration buildRegistration(User user, Event event) {
        return Registration.builder()
                .user(user)
                .event(event)
                .gdprConsent(true)
                .transportationNeeded(false)
                .accommodationNeeded(false)
                .build();
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

    @Test
    void exportThrowsIllegalArgumentExceptionWhenEventNotFound() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventExportService.exportEventRegistrationsToExcel(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event not found");
    }

    @Test
    void exportThrowsInvalidEventOperationExceptionWhenEventIsDraft() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.of(buildEvent(EventStatus.DRAFT)));

        assertThatThrownBy(() -> eventExportService.exportEventRegistrationsToExcel(id))
                .isInstanceOf(InvalidEventOperationException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void exportReturnsNonEmptyBytesWhenEventIsPublished() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.of(buildEvent(EventStatus.PUBLISHED)));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of());

        byte[] result = eventExportService.exportEventRegistrationsToExcel(id);

        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    void exportReturnsNonEmptyBytesWhenEventIsCompleted() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.of(buildEvent(EventStatus.COMPLETED)));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of());

        byte[] result = eventExportService.exportEventRegistrationsToExcel(id);

        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    void exportWritesHeaderRowWithExactColumnNames() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.of(buildEvent(EventStatus.PUBLISHED)));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of());

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));
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
    }

    @Test
    void exportAssignsSequentialNrCrtStartingAt1() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User u1 = buildUser("alice.brown@msg.group", "Alice", "Brown");
        User u2 = buildUser("charlie.davis@msg.group", "Charlie", "Davis");
        User u3 = buildUser("eve.ford@msg.group", "Eve", "Ford");

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of(
                buildRegistration(u1, event),
                buildRegistration(u2, event),
                buildRegistration(u3, event)));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));

        assertThat(cellString(sheet.getRow(1), 0)).isEqualTo("1");
        assertThat(cellString(sheet.getRow(2), 0)).isEqualTo("2");
        assertThat(cellString(sheet.getRow(3), 0)).isEqualTo("3");
    }

    @Test
    void exportPopulatesNameEmailAndEventNameCorrectly() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User user = buildUser("alice.brown@msg.group", "Alice", "Brown");

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id))
                .thenReturn(List.of(buildRegistration(user, event)));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));
        Row row = sheet.getRow(1);

        assertThat(cellString(row, 1)).isEqualTo("Brown");
        assertThat(cellString(row, 2)).isEqualTo("Alice");
        assertThat(cellString(row, 3)).isEqualTo("Tech Summit 2026");
        assertThat(cellString(row, 4)).isEqualTo("alice.brown@msg.group");
    }

    @Test
    void exportSetsEventNameOnEveryRowFromEventEntity() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User u1 = buildUser("a.b@msg.group", "A", "B");
        User u2 = buildUser("c.d@msg.group", "C", "D");

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of(
                buildRegistration(u1, event),
                buildRegistration(u2, event)));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));

        assertThat(cellString(sheet.getRow(1), 3)).isEqualTo("Tech Summit 2026");
        assertThat(cellString(sheet.getRow(2), 3)).isEqualTo("Tech Summit 2026");
    }

    @Test
    void exportSetsFoodPreferenceEmptyWhenNone() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User user = buildUser("a.b@msg.group", "A", "B");
        Registration reg = Registration.builder()
                .user(user).event(event).gdprConsent(true)
                .foodPreference(FoodPreference.NONE)
                .transportationNeeded(false).accommodationNeeded(false)
                .build();

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of(reg));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));
        assertThat(cellString(sheet.getRow(1), 5)).isEmpty();
    }

    @Test
    void exportSetsFoodPreferenceWhenVegetarian() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User user = buildUser("a.b@msg.group", "A", "B");
        Registration reg = Registration.builder()
                .user(user).event(event).gdprConsent(true)
                .foodPreference(FoodPreference.VEGETARIAN)
                .transportationNeeded(false).accommodationNeeded(false)
                .build();

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of(reg));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));
        assertThat(cellString(sheet.getRow(1), 5)).isEqualTo("VEGETARIAN");
    }

    @Test
    void exportSetsFoodPreferenceWhenVegan() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User user = buildUser("a.b@msg.group", "A", "B");
        Registration reg = Registration.builder()
                .user(user).event(event).gdprConsent(true)
                .foodPreference(FoodPreference.VEGAN)
                .transportationNeeded(false).accommodationNeeded(false)
                .build();

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of(reg));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));
        assertThat(cellString(sheet.getRow(1), 5)).isEqualTo("VEGAN");
    }

    @Test
    void exportSetsFoodPreferenceEmptyWhenNull() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User user = buildUser("a.b@msg.group", "A", "B");
        Registration reg = Registration.builder()
                .user(user).event(event).gdprConsent(true)
                .foodPreference(null)
                .transportationNeeded(false).accommodationNeeded(false)
                .build();

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of(reg));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));
        assertThat(cellString(sheet.getRow(1), 5)).isEmpty();
    }

    @Test
    void exportSetsTransportNoAndEmptyDriverFieldsWhenNotRequired() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User user = buildUser("a.b@msg.group", "A", "B");
        Registration reg = buildRegistration(user, event); // transportationNeeded = false

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of(reg));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));
        Row row = sheet.getRow(1);

        assertThat(cellString(row, 6)).isEqualTo("no");
        assertThat(cellString(row, 8)).isEmpty();
        assertThat(cellString(row, 9)).isEmpty();
    }

    @Test
    void exportSetsTransportYesAndPopulatesDriverFieldsWhenRequired() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User user = buildUser("a.b@msg.group", "A", "B");
        Registration reg = Registration.builder()
                .user(user).event(event).gdprConsent(true)
                .transportationNeeded(true).accommodationNeeded(false)
                .build();
        TransportationDetails td = TransportationDetails.builder()
                .registration(reg)
                .driverName("John Driver")
                .driverPhoneNumber("0740000000")
                .build();
        reg.setTransportationDetails(td);

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of(reg));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));
        Row row = sheet.getRow(1);

        assertThat(cellString(row, 6)).isEqualTo("yes");
        assertThat(cellString(row, 8)).isEqualTo("John Driver");
        assertThat(cellString(row, 9)).isEqualTo("0740000000");
    }

    @Test
    void exportSetsEmptyDriverFieldsWhenTransportRequiredButDetailsAreNull() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User user = buildUser("a.b@msg.group", "A", "B");
        Registration reg = Registration.builder()
                .user(user).event(event).gdprConsent(true)
                .transportationNeeded(true).accommodationNeeded(false)
                .transportationDetails(null)
                .build();

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of(reg));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));
        Row row = sheet.getRow(1);

        assertThat(cellString(row, 8)).isEmpty();
        assertThat(cellString(row, 9)).isEmpty();
    }

    @Test
    void exportSetsAccommodationNoWhenNotRequired() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User user = buildUser("a.b@msg.group", "A", "B");

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id))
                .thenReturn(List.of(buildRegistration(user, event)));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));
        assertThat(cellString(sheet.getRow(1), 7)).isEqualTo("no");
    }

    @Test
    void exportSetsAccommodationYesWithDayCountWhenRequiredAndDaysProvided() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User user = buildUser("a.b@msg.group", "A", "B");
        Registration reg = Registration.builder()
                .user(user).event(event).gdprConsent(true)
                .transportationNeeded(false).accommodationNeeded(true).accommodationDays(3)
                .build();

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of(reg));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));
        assertThat(cellString(sheet.getRow(1), 7)).isEqualTo("yes (3 days)");
    }

    @Test
    void exportSetsAccommodationYesWithoutDayCountWhenDaysAreNull() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User user = buildUser("a.b@msg.group", "A", "B");
        Registration reg = Registration.builder()
                .user(user).event(event).gdprConsent(true)
                .transportationNeeded(false).accommodationNeeded(true).accommodationDays(null)
                .build();

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of(reg));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));
        assertThat(cellString(sheet.getRow(1), 7)).isEqualTo("yes");
    }

    @Test
    void exportSetsGdprYesWhenConsentIsTrue() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User user = buildUser("a.b@msg.group", "A", "B");
        Registration reg = Registration.builder()
                .user(user).event(event).gdprConsent(true)
                .transportationNeeded(false).accommodationNeeded(false)
                .build();

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of(reg));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));
        assertThat(cellString(sheet.getRow(1), 10)).isEqualTo("yes");
    }

    @Test
    void exportSetsGdprNoWhenConsentIsFalse() throws Exception {
        UUID id = UUID.randomUUID();
        Event event = buildEvent(EventStatus.PUBLISHED);
        User user = buildUser("a.b@msg.group", "A", "B");
        Registration reg = Registration.builder()
                .user(user).event(event).gdprConsent(false)
                .transportationNeeded(false).accommodationNeeded(false)
                .build();

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdWithDetails(id)).thenReturn(List.of(reg));

        Sheet sheet = parseFirstSheet(eventExportService.exportEventRegistrationsToExcel(id));
        assertThat(cellString(sheet.getRow(1), 10)).isEqualTo("no");
    }
}
