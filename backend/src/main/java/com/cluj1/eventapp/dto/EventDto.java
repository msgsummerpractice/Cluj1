package com.cluj1.eventapp.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Event name is required.")
    @Size(max = 100, message = "Event name must not exceed 100 characters.")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters.")
    private String description;

    @NotNull(message = "Start date is required.")
    private OffsetDateTime startDate;

    @NotNull(message = "End date is required.")
    private OffsetDateTime endDate;

    private OffsetDateTime registrationEndDate;
    private EventLocation location;
    private EventType type;
    private Boolean foodProvided;
    private EventStatus status;
    private Boolean isRegistered;
    private Boolean isCheckedIn;
    private Boolean checkInEnabled;
}