package com.cluj1.eventapp.mapper;

import org.springframework.stereotype.Component;
import com.cluj1.eventapp.dto.EventDto;
import com.cluj1.eventapp.model.Event;

@Component
public class EventMapper {

    public EventDto toDto(Event event) {
        return EventDto.builder()
                .id(event.getId())
                .name(event.getName())
                .startDate(event.getEventStartDate())
                .endDate(event.getEventEndTime())
                .registrationEndDate(event.getRegistrationEndDate())
                .location(event.getLocation() != null ? event.getLocation().name() : null)
                .type(event.getType())
                .status(event.getStatus())
                .build();
    }
}
