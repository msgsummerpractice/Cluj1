package com.cluj1.eventapp.mapper;

import com.cluj1.eventapp.dto.UserDTO;
import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.UserDetails;
import com.cluj1.eventapp.model.enums.UserLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
@Component
public class UserMapper {

    private final PasswordEncoder passwordEncoder;

    public User mapToEntity(UserRegistrationDto userRegistrationDto){
        User user = new User();
        user.setEmail(userRegistrationDto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(userRegistrationDto.getPassword()));
        user.setCreatedAt(OffsetDateTime.now());

        UserDetails details = new UserDetails();
        details.setFirstName(userRegistrationDto.getFirstName());
        details.setLastName(userRegistrationDto.getFirstName());
        details.setLocation(UserLocation.valueOf(userRegistrationDto.getUserLocation().name()));

        details.setUser(user);
        user.setUserDetails(details);
        return user;
    }
    public UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .firstName(user.getUserDetails() != null ? user.getUserDetails().getFirstName() : null)
                .lastName(user.getUserDetails() != null ? user.getUserDetails().getLastName() : null)
                .location(user.getUserDetails() != null ? user.getUserDetails().getLocation() : null)
                .build();
    }
}
