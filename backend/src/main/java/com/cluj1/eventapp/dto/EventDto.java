package com.cluj1.eventapp.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.cluj1.eventapp.model.enums.EventType;

import lombok.Builder;
import lombok.Getter;

import com.cluj1.eventapp.model.enums.EventStatus;

@Getter
@Builder
public class EventDto {
    private UUID id;
    private String name;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private String location;
    private EventType type;
    private EventStatus status;
}
