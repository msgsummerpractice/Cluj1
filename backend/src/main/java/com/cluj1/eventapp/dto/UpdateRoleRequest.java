package com.cluj1.eventapp.dto;

import com.cluj1.eventapp.model.enums.Role;

import lombok.Data;

@Data
public class UpdateRoleRequest {
    private Role role;
}
