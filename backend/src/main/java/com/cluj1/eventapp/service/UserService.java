package com.cluj1.eventapp.service;

import java.util.List;
import java.util.stream.Collectors;

import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.exception.EmailAlreadyRegisteredException;
import com.cluj1.eventapp.mapper.UserMapper;
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
}
