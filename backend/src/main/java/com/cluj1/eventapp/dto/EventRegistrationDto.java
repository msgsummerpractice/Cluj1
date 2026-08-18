package com.cluj1.eventapp.dto;

import com.cluj1.eventapp.model.enums.FoodPreference;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventRegistrationDto {

    @NotNull(message = "GDPR consent is required")
    private Boolean gdprConsent;

    @NotNull(message = "Photo consent is required")
    private Boolean photoConsent;

    private FoodPreference foodPreference;

    private Boolean transportationNeeded;
    private String driverName;
    private String driverPhone;

    private Boolean accommodationNeeded;
    private Integer accommodationDays;
}