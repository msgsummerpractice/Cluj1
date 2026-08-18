package com.cluj1.eventapp.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventDetailsDto {
    private UUID id;
    private UUID eventId;
    private String description;
    private Boolean foodProvided;
    private String eventCode;
    private String qrCodeContent;
}
