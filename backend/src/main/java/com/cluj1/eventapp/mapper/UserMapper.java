package com.cluj1.eventapp.mapper;

import org.springframework.stereotype.Component;

import com.cluj1.eventapp.dto.UserDto;
import com.cluj1.eventapp.model.User;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        return UserDto.builder()
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
