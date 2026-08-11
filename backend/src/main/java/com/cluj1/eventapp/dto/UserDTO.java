package com.cluj1.eventapp.dto;

import com.cluj1.eventapp.model.Role;
import com.cluj1.eventapp.model.UserLocation;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private UserLocation location;
    private Boolean isActive;
}
