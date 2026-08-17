package com.cluj1.eventapp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.exception.EmailAlreadyRegisteredException;
import com.cluj1.eventapp.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.cluj1.eventapp.dto.UserDTO;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.model.enums.UserLocation;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Mock
    private UserMapper mapper;

    @Test
    void getAllUsers_returnMappedUsers_whenUsersExist() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("john.doe@msg.group");
        user.setRole(Role.PARTICIPANT);
        user.setIsActive(true);

        UserDTO expectedDto = UserDTO.builder()
                .email("john.doe@msg.group")
                .firstName("John")
                .lastName("Doe")
                .role(Role.PARTICIPANT)
                .location(UserLocation.values()[0])
                .build();

        when(userRepository.searchUsers(any(), any())).thenReturn(new PageImpl<>(Arrays.asList(user)));
        when(mapper.mapToDTO(user)).thenReturn(expectedDto);

        Page<UserDTO> result = userService.getAllUsers("John", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("John", result.getContent().get(0).getFirstName());
        assertEquals("Doe", result.getContent().get(0).getLastName());
        assertEquals("john.doe@msg.group", result.getContent().get(0).getEmail());
        assertEquals(Role.PARTICIPANT, result.getContent().get(0).getRole());
        verify(userRepository, times(1)).searchUsers(any(), any());
    }

    @Test
    void getAllUsers_handleNullUserDetails_gracefully() {
        User userWithoutDetails = new User();
        userWithoutDetails.setId(UUID.randomUUID());
        userWithoutDetails.setEmail("ghost.admin@msg.group");
        userWithoutDetails.setRole(Role.ADMIN);
        userWithoutDetails.setIsActive(false);
        userWithoutDetails.setUserDetails(null);

        UserDTO expectedDto = UserDTO.builder()
                .email("ghost.admin@msg.group")
                .role(Role.ADMIN)
                .isActive(false)
                .build();

        when(userRepository.searchUsers(any(), any())).thenReturn(new PageImpl<>(Arrays.asList(userWithoutDetails)));
        when(mapper.mapToDTO(userWithoutDetails)).thenReturn(expectedDto);

        Page<UserDTO> result = userService.getAllUsers(null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertNull(result.getContent().get(0).getFirstName());
        assertNull(result.getContent().get(0).getLastName());
        assertNull(result.getContent().get(0).getLocation());
        assertEquals("ghost.admin@msg.group", result.getContent().get(0).getEmail());
    }

    @Test
    void getAllUsers_returnEmptyList_whenNoMatchFound() {
        when(userRepository.searchUsers(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        Page<UserDTO> result = userService.getAllUsers("NonExistent", PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }

    @Test
    void registerUser_saveUserWhenEmailIsNotRegistered() {
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
    void registerUser_throwExceptionWhenEmailIsAlreadyRegistered() {
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

    @Test
    void updateUserStatus_deactivateUser_whenCurrentlyActive() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId).email("active.user@msg.group").passwordHash("hash")
                .role(Role.PARTICIPANT).isActive(true).build();
        UserDTO expectedDto = UserDTO.builder().id(userId).role(Role.PARTICIPANT).isActive(false).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mapper.mapToDTO(user)).thenReturn(expectedDto);

        UserDTO result = userService.updateUserStatus(userId, false);

        assertFalse(result.getIsActive());
        assertFalse(user.getIsActive());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserStatus_activateUser_whenCurrentlyInactive() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId).email("inactive.user@msg.group").passwordHash("hash")
                .role(Role.PARTICIPANT).isActive(false).build();
        UserDTO expectedDto = UserDTO.builder().id(userId).role(Role.PARTICIPANT).isActive(true).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mapper.mapToDTO(user)).thenReturn(expectedDto);

        UserDTO result = userService.updateUserStatus(userId, true);

        assertTrue(result.getIsActive());
        assertTrue(user.getIsActive());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserStatus_throw404_whenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.updateUserStatus(userId, false));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserStatus_throw400_whenDeactivatingLastActiveAdmin() {
        UUID userId = UUID.randomUUID();
        User admin = User.builder()
                .id(userId).email("sole.admin@msg.group").passwordHash("hash")
                .role(Role.ADMIN).isActive(true).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndIsActiveTrue(Role.ADMIN)).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.updateUserStatus(userId, false));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserStatus_succeed_whenDeactivatingAdminWithMultipleActiveAdmins() {
        UUID userId = UUID.randomUUID();
        User admin = User.builder()
                .id(userId).email("one.admin@msg.group").passwordHash("hash")
                .role(Role.ADMIN).isActive(true).build();
        UserDTO expectedDto = UserDTO.builder().id(userId).role(Role.ADMIN).isActive(false).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndIsActiveTrue(Role.ADMIN)).thenReturn(2L);
        when(mapper.mapToDTO(admin)).thenReturn(expectedDto);

        UserDTO result = userService.updateUserStatus(userId, false);

        assertFalse(result.getIsActive());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRole_changeRoleSuccessfully() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId).email("participant@msg.group").passwordHash("hash")
                .role(Role.PARTICIPANT).isActive(true).build();
        UserDTO expectedDto = UserDTO.builder().id(userId).role(Role.ADMIN).isActive(true).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mapper.mapToDTO(user)).thenReturn(expectedDto);

        UserDTO result = userService.updateUserRole(userId, Role.ADMIN);

        assertEquals(Role.ADMIN, result.getRole());
        assertEquals(Role.ADMIN, user.getRole());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRole_throw404_whenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.updateUserRole(userId, Role.ADMIN));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRole_throw400_whenRemovingRoleFromLastActiveAdmin() {
        UUID userId = UUID.randomUUID();
        User admin = User.builder()
                .id(userId).email("sole.admin@msg.group").passwordHash("hash")
                .role(Role.ADMIN).isActive(true).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndIsActiveTrue(Role.ADMIN)).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.updateUserRole(userId, Role.PARTICIPANT));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }
}