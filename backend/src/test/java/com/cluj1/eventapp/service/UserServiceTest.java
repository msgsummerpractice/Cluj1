package com.cluj1.eventapp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

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

        when(userRepository.searchUsers("John")).thenReturn(Arrays.asList(user));

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

        when(userRepository.searchUsers(null)).thenReturn(Arrays.asList(userWithoutDetails));

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
}