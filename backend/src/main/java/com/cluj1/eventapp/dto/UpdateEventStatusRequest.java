package com.cluj1.eventapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateEventStatusRequest {
    @NotBlank(message = "Status cannot be blank")
    private String status;
}
