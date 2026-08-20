package com.cluj1.eventapp.mapper;

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
}