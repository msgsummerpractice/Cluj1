package com.cluj1.eventapp.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cluj1.eventapp.dto.UserDTO;
import com.cluj1.eventapp.model.User;

import com.cluj1.eventapp.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers(String searchTerm) {
        List<User> users = userRepository.searchUsers(searchTerm);

        return users.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private UserDTO mapToDTO(User user) {
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
