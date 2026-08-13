package com.cluj1.eventapp.service;

import com.cluj1.eventapp.dto.UserProfileDto;
import com.cluj1.eventapp.mapper.UserMapper;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.UserDetails;
import com.cluj1.eventapp.repository.UserDetailsRepository;
import com.cluj1.eventapp.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsService {

    private final UserRepository userRepository;
    private final UserDetailsRepository userDetailsRepository;

    private final UserMapper mapper;

    public UserDetails getUserDetailsByUserId(String userId) {
        return userDetailsRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User details not found for user id: " + userId));
    }

    public UserProfileDto getUserProfile(String userId) {
        UserDetails userDetails = getUserDetailsByUserId(userId);
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found for user id: " + userId));
        return mapper.mapUserToUserProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateUserProfile(String userId, UserProfileDto newUserProfileDto) {
        UserDetails userDetails = getUserDetailsByUserId(userId);
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found for user id: " + userId));
        if(newUserProfileDto.getUserLocation() != null) {
            userDetails.setLocation(newUserProfileDto.getUserLocation());
        }
        if(newUserProfileDto.getProfilePicture() != null) {
            userDetails.setProfilePicture(newUserProfileDto.getProfilePicture());
        }

        return UserProfileDto.builder()
                .firstName(userDetails.getFirstName())
                .lastName(userDetails.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .userLocation(userDetails.getLocation())
                .profilePicture(userDetails.getProfilePicture())
                .build();
    }

}
