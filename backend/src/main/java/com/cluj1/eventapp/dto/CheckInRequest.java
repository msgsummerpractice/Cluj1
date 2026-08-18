package com.cluj1.eventapp.dto;

import com.cluj1.eventapp.model.enums.CheckInMethod;

import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckInRequest {

    private UUID eventId;

    private String eventCode;

    @NotNull(message = "checkin.error.method.required")
    private CheckInMethod method;

    @AssertTrue(message = "checkin.error.code.invalid")
    public boolean hasValidIdentifier() {
        if (method == null) {
            return true;
        }

        return switch (method) {
            case QR -> eventId != null && eventCode == null;
            case MANUAL -> eventId == null && eventCode != null && eventCode.trim().length() == 6;
        };
    }
}
