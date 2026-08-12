package com.cluj1.eventapp.service;

import com.cluj1.eventapp.dto.AuthResponse;
import com.cluj1.eventapp.dto.LogInRequest;
import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.exception.EmailAlreadyRegisteredException;
import com.cluj1.eventapp.mapper.DtoMapper;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.repository.UserRepository;
import com.cluj1.eventapp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final DtoMapper mapper;

    public AuthResponse login(LogInRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        String token = tokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .build();
    }

    public void registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = mapper.mapToEntity(registrationDto);
        userRepository.save(user);
    }
}