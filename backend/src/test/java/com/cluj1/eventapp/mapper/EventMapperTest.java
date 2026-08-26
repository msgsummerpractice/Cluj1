package com.cluj1.eventapp.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cluj1.eventapp.dto.EventDto;
import com.cluj1.eventapp.dto.EventRegistrationDto;
import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.EventDetails;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.model.TransportationDetails;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;
import com.cluj1.eventapp.model.enums.FoodPreference;

class EventMapperTest {

    private EventMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EventMapper();
    }

    @Test
    void toDto_mapsAllFields_whenEventDetailsPresent() {
        UUID id = UUID.randomUUID();
        Event event = Event.builder()
                .id(id)
                .name("Tech Meetup")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.PUBLISHED)
                .eventStartDate(OffsetDateTime.now())
                .eventEndTime(OffsetDateTime.now().plusHours(2))
                .registrationEndDate(OffsetDateTime.now().minusDays(1))
                .build();
        EventDetails details = EventDetails.builder()
                .description("desc")
                .foodProvided(true)
                .eventCode("ABC123")
                .build();
        event.setEventDetails(details);

        EventDto dto = mapper.toDto(event);

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getName()).isEqualTo("Tech Meetup");
        assertThat(dto.getDescription()).isEqualTo("desc");
        assertThat(dto.getFoodProvided()).isTrue();
        assertThat(dto.getLocation()).isEqualTo(EventLocation.CLUJ);
        assertThat(dto.getType()).isEqualTo(EventType.LOCAL);
        assertThat(dto.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(dto.getCheckInEnabled()).isTrue();
    }

    @Test
    void toDto_nullsOutDetails_whenEventDetailsMissing() {
        Event event = Event.builder()
                .id(UUID.randomUUID())
                .name("No Details")
                .location(EventLocation.ALL)
                .type(EventType.INTERNAL)
                .status(EventStatus.DRAFT)
                .build();

        EventDto dto = mapper.toDto(event);

        assertThat(dto.getDescription()).isNull();
        assertThat(dto.getFoodProvided()).isNull();
        assertThat(dto.getCheckInEnabled()).isFalse();
    }

    @Test
    void toDto_disablesCheckIn_whenNotPublished() {
        Event event = Event.builder()
                .id(UUID.randomUUID())
                .name("Draft Event")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.DRAFT)
                .build();
        event.setEventDetails(EventDetails.builder().eventCode("XYZ789").foodProvided(false).build());

        EventDto dto = mapper.toDto(event);

        assertThat(dto.getCheckInEnabled()).isFalse();
    }

    @Test
    void toDto_disablesCheckIn_whenPublishedButEventCodeMissing() {
        Event event = Event.builder()
                .id(UUID.randomUUID())
                .name("Missing Code")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.PUBLISHED)
                .build();
        event.setEventDetails(EventDetails.builder().foodProvided(true).build());

        EventDto dto = mapper.toDto(event);

        assertThat(dto.getCheckInEnabled()).isFalse();
    }

    @Test
    void toEventRegistrationDto_mapsAllFields_whenTransportationDetailsPresent() {
        Registration registration = Registration.builder()
                .gdprConsent(true)
                .photoConsent(false)
                .foodPreference(FoodPreference.VEGAN)
                .transportationNeeded(true)
                .accommodationNeeded(true)
                .accommodationDays(2)
                .build();
        TransportationDetails transport = TransportationDetails.builder()
                .driverName("Jane Driver")
                .driverPhoneNumber("+40123456789")
                .build();
        registration.setTransportationDetails(transport);

        EventRegistrationDto dto = mapper.toEventRegistrationDto(registration);

        assertThat(dto.getGdprConsent()).isTrue();
        assertThat(dto.getPhotoConsent()).isFalse();
        assertThat(dto.getFoodPreference()).isEqualTo(FoodPreference.VEGAN);
        assertThat(dto.getTransportationNeeded()).isTrue();
        assertThat(dto.getAccommodationNeeded()).isTrue();
        assertThat(dto.getAccommodationDays()).isEqualTo(2);
        assertThat(dto.getDriverName()).isEqualTo("Jane Driver");
        assertThat(dto.getDriverPhone()).isEqualTo("+40123456789");
    }

    @Test
    void toEventRegistrationDto_nullsDriverFields_whenTransportationDetailsMissing() {
        Registration registration = Registration.builder()
                .gdprConsent(true)
                .photoConsent(true)
                .build();

        EventRegistrationDto dto = mapper.toEventRegistrationDto(registration);

        assertThat(dto.getDriverName()).isNull();
        assertThat(dto.getDriverPhone()).isNull();
    }
}

