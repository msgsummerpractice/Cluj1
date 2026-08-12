package com.cluj1.eventapp.service;

import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.exception.EmailAlreadyRegisteredException;
import com.cluj1.eventapp.mapper.DtoMapper;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private DtoMapper mapper;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerUser_ShouldSaveUserWhenEmailIsNotRegistered() {
        String email = "test@example.com";

        UserRegistrationDto dto = mock(UserRegistrationDto.class);
        when(dto.getEmail()).thenReturn(email);

        User mappedUser = new User();

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(mapper.mapToEntity(dto)).thenReturn(mappedUser);

        authService.registerUser(dto);
        verify(userRepository, times(1)).save(mappedUser);
    }

    @Test
    void registerUser_ShouldThrowExceptionWhenEmailIsAlreadyRegistered() {

        String email = "duplicate@example.com";

        UserRegistrationDto dto = mock(UserRegistrationDto.class);
        when(dto.getEmail()).thenReturn(email);

        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> {
            authService.registerUser(dto);
        });

        verify(mapper, never()).mapToEntity(any());
        verify(userRepository, never()).save(any());
    }
}