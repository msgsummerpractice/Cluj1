package com.cluj1.eventapp.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cluj1.eventapp.dto.UserProfileDto;
import com.cluj1.eventapp.dto.UserProfileUpdateDto;
import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.exception.EmailAlreadyRegisteredException;
import com.cluj1.eventapp.mapper.UserMapper;
import com.cluj1.eventapp.model.UserDetails;
import org.springframework.stereotype.Service;

import com.cluj1.eventapp.dto.UserDTO;
import com.cluj1.eventapp.model.User;

import com.cluj1.eventapp.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper mapper;

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers(String searchTerm) {
        List<User> users = userRepository.searchUsers(searchTerm);

        return users.stream()
                .map(mapper::mapToDTO)
                .collect(Collectors.toList());
    }

    public void registerUser(UserRegistrationDto registrationDto) {
        registrationDto.setEmail(registrationDto.getEmail().toLowerCase());
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = mapper.mapToEntity(registrationDto);
        userRepository.save(user);
    }


    @Transactional(readOnly = true)
    public UserProfileDto getUserProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + email));
        return mapper.mapUserToUserProfileDto(user);
    }

    @Transactional
    public void updateUserProfile(String email, UserProfileUpdateDto updateDto) throws IOException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found with id: " + email));
        UserDetails details = user.getUserDetails();
        if(details == null) {
            details = new UserDetails();
            details.setUser(user);
            user.setUserDetails(details);
        }
        if(updateDto.getUserLocation() != null) {
            details.setLocation(updateDto.getUserLocation());
        }
        if(updateDto.getProfilePicture() != null && !updateDto.getProfilePicture().isEmpty()) {
            details.setProfilePicture(updateDto.getProfilePicture().getBytes());
        }

        user.setUpdatedAt(java.time.OffsetDateTime.now());
        userRepository.save(user);

    }
}
