package com.cluj1.eventapp.dto;

import com.cluj1.eventapp.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter @Builder @AllArgsConstructor
public class AuthResponse {
    private String token;
    private UUID userId;
    private String email;
    private Role role;
}