package com.cluj1.eventapp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.exception.EmailAlreadyRegisteredException;
import com.cluj1.eventapp.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cluj1.eventapp.dto.UserDTO;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.model.enums.UserLocation;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.UserDetails;
import com.cluj1.eventapp.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper mapper;

    @InjectMocks
    private UserService userService;


    @Test
    void getAllUsers_ShouldReturnMappedUsers_WhenUsersExist() {
        UserDetails details = new UserDetails();
        details.setFirstName("John");
        details.setLastName("Doe");
        details.setLocation(UserLocation.values()[0]);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("john.doe@msg.com");
        user.setRole(Role.PARTICIPANT);
        user.setIsActive(true);
        user.setUserDetails(details);

        UserDTO dto = UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(true)
                .firstName("John")
                .lastName("Doe")
                .location(UserLocation.values()[0])
                .build();

        when(userRepository.searchUsers("John")).thenReturn(Arrays.asList(user));
        when(mapper.mapToDTO(user)).thenReturn(dto);

        List<UserDTO> result = userService.getAllUsers("John");

        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
        assertEquals("Doe", result.get(0).getLastName());
        assertEquals("john.doe@msg.com", result.get(0).getEmail());
        assertEquals(UserLocation.values()[0], result.get(0).getLocation());
        assertEquals(Role.PARTICIPANT, result.get(0).getRole());
        verify(userRepository, times(1)).searchUsers("John");
    }

    @Test
    void getAllUsers_ShouldHandleNullUserDetails_Gracefully() {
        User userWithoutDetails = new User();
        userWithoutDetails.setId(UUID.randomUUID());
        userWithoutDetails.setEmail("ghost.admin@msg.com");
        // Role enum needs to match one of your actual roles, assuming ADMIN exists
        userWithoutDetails.setRole(Role.valueOf("ADMIN") != null ? Role.valueOf("ADMIN") : Role.PARTICIPANT);
        userWithoutDetails.setIsActive(false);
        userWithoutDetails.setUserDetails(null);

        UserDTO dto = UserDTO.builder()
                .id(userWithoutDetails.getId())
                .email(userWithoutDetails.getEmail())
                .role(userWithoutDetails.getRole())
                .isActive(false)
                .firstName(null)
                .lastName(null)
                .location(null)
                .build();

        when(userRepository.searchUsers(null)).thenReturn(Arrays.asList(userWithoutDetails));
        when(mapper.mapToDTO(userWithoutDetails)).thenReturn(dto);

        List<UserDTO> result = userService.getAllUsers(null);

        assertEquals(1, result.size());
        assertNull(result.get(0).getFirstName());
        assertNull(result.get(0).getLastName());
        assertNull(result.get(0).getLocation());
        assertEquals("ghost.admin@msg.com", result.get(0).getEmail());
    }

    @Test
    void getAllUsers_ShouldReturnEmptyList_WhenNoMatchFound() {
        when(userRepository.searchUsers("NonExistent")).thenReturn(Collections.emptyList());

        List<UserDTO> result = userService.getAllUsers("NonExistent");

        assertTrue(result.isEmpty());
    }
    @Test
    void registerUser_ShouldSaveUserWhenEmailIsNotRegistered() {
        String email = "test.user@msg.group";

        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail(email);

        User mappedUser = new User();

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(mapper.mapToEntity(dto)).thenReturn(mappedUser);

        userService.registerUser(dto);
        verify(userRepository, times(1)).save(mappedUser);
    }

    @Test
    void registerUser_ShouldThrowExceptionWhenEmailIsAlreadyRegistered() {
        String email = "duplicate.user@msg.group";

        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail(email);

        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> {
            userService.registerUser(dto);
        });

        verify(mapper, never()).mapToEntity(any());
        verify(userRepository, never()).save(any());
    }
}