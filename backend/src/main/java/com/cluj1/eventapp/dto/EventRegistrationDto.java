package com.cluj1.eventapp.dto;

import com.cluj1.eventapp.model.enums.FoodPreference;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EventRegistrationDto {

    @NotNull(message = "GDPR consent is required")
    private Boolean gdprConsent;

    @NotNull(message = "Photo consent is required")
    private Boolean photoConsent;

    private FoodPreference foodPreference;

    private Boolean transportationNeeded;

    @Size(max = 255, message = "Driver name must not exceed 255 characters")
    @Pattern(regexp = "^[\\p{L}'-]+$", message = "Driver name may contain only letters, hyphens, and apostrophes")
    private String driverName;

    @Size(max = 50, message = "Driver phone must not exceed 50 characters")
    @Pattern(regexp = "^\\+?[0-9]+$", message = "Driver phone must contain an optional leading plus sign followed by digits")
    private String driverPhone;

    private Boolean accommodationNeeded;
    private Integer accommodationDays;
}