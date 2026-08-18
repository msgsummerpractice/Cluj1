package com.cluj1.eventapp.dto;

import com.cluj1.eventapp.model.enums.CheckInMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CheckInRequest {

    @NotBlank(message = "checkin.error.code.required")
    @Size(min = 6, max = 255, message = "checkin.error.code.invalid")
    private String code;

    @NotNull(message = "checkin.error.method.required")
    private CheckInMethod method;
}
