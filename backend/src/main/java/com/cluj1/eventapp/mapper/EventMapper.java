package com.cluj1.eventapp.mapper;

import com.cluj1.eventapp.dto.EventRegistrationDto;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.repository.EventRepository;
import org.springframework.stereotype.Component;
import com.cluj1.eventapp.dto.EventDto;
import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.enums.EventStatus;

@Component
public class EventMapper {

    public EventDto toDto(Event event) {
        return EventDto.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getEventDetails() != null ? event.getEventDetails().getDescription() : null)
                .foodProvided(event.getEventDetails() != null ? event.getEventDetails().getFoodProvided() : null)
                .startDate(event.getEventStartDate())
                .endDate(event.getEventEndTime())
                .registrationEndDate(event.getRegistrationEndDate())
                .location(event.getLocation())
                .type(event.getType())
                .status(event.getStatus())
                .checkInEnabled(
                        event.getStatus() == EventStatus.PUBLISHED
                                && event.getEventDetails() != null
                                && event.getEventDetails().getEventCode() != null)
                .build();
    }

    public EventRegistrationDto toEventRegistrationDto(Registration registration) {
        EventRegistrationDto dto = new EventRegistrationDto();
        dto.setGdprConsent(registration.getGdprConsent());
        dto.setPhotoConsent(registration.getPhotoConsent());
        dto.setFoodPreference(registration.getFoodPreference());
        dto.setTransportationNeeded(registration.getTransportationNeeded());
        dto.setAccommodationNeeded(registration.getAccommodationNeeded());
        dto.setAccommodationDays(registration.getAccommodationDays());

        if (registration.getTransportationDetails() != null) {
            dto.setDriverName(registration.getTransportationDetails().getDriverName());
            dto.setDriverPhone(registration.getTransportationDetails().getDriverPhoneNumber());
        }
        return dto;
    }
}