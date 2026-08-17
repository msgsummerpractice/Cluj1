package com.cluj1.eventapp.dto;

import com.cluj1.eventapp.model.enums.Role;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRoleRequest {
    @NotNull(message = "Role cannot be null")
    private Role role;
}
