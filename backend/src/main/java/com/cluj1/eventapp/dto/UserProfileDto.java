package com.cluj1.eventapp.dto;

import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.model.enums.UserLocation;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class UserProfileDto {

    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private UserLocation userLocation;
    private byte[] profilePicture;

}
