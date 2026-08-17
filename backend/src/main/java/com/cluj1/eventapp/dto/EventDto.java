package com.cluj1.eventapp.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private UUID id;
    private String name;
    private String description;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private OffsetDateTime registrationEndDate;
    private EventLocation location;
    private EventType type;
    private Boolean foodProvided;
    private EventStatus status;
}