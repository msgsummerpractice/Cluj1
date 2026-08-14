package com.cluj1.eventapp.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.exception.EmailAlreadyRegisteredException;
import com.cluj1.eventapp.mapper.UserMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cluj1.eventapp.dto.UserDTO;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.repository.UserRepository;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper mapper;

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(String searchTerm, Pageable pageable) {
        Page<User> users = userRepository.searchUsers(searchTerm, pageable);
        return users.map(mapper::mapToDTO);
    }

    public void registerUser(UserRegistrationDto registrationDto) {
        registrationDto.setEmail(registrationDto.getEmail().toLowerCase());
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = mapper.mapToEntity(registrationDto);
        userRepository.save(user);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDTO updateUserRole(UUID userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            validateAdminCount();
        }

        user.setRole(newRole);
        return mapper.mapToDTO(user);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDTO updateUserStatus(UUID userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == Role.ADMIN && !isActive) {
            validateAdminCount();
        }

        user.setIsActive(isActive);
        return mapper.mapToDTO(user);
    }

    private void validateAdminCount() {
        if (userRepository.countByRoleAndIsActiveTrue(Role.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot remove or deactivate the last active Admin account.");
        }
    }
}
