package com.cluj1.eventapp.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cluj1.eventapp.dto.UserDto;
import com.cluj1.eventapp.mapper.UserMapper;

import com.cluj1.eventapp.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers(String searchTerm) {
        return userRepository.searchUsers(searchTerm).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }
}
