package com.cluj1.eventapp.mapper;

import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class DtoMapper {

    private final PasswordEncoder passwordEncoder;

    public User mapToEntity(UserRegistrationDto userRegistrationDto){
        User user = new User();
        user.setEmail(userRegistrationDto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(userRegistrationDto.getPassword()));
        user.setCreatedAt(OffsetDateTime.now());
        return user;
    }
}
