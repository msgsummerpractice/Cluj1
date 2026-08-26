package com.cluj1.eventapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RegistrationService registrationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("john.doe@msg.group")
                .build();
    }

    @Test
    void getRegistrationsPerUserByEmail_returnsCount_whenUserExists() {
        when(userRepository.findByEmail("john.doe@msg.group")).thenReturn(Optional.of(user));
        when(registrationRepository.countTotalRegistrationsPerUser(user.getId())).thenReturn(5);

        int result = registrationService.getRegistrationsPerUserByEmail("john.doe@msg.group");

        assertThat(result).isEqualTo(5);
        verify(userRepository).findByEmail("john.doe@msg.group");
        verify(registrationRepository).countTotalRegistrationsPerUser(user.getId());
    }

    @Test
    void getRegistrationsPerUserByEmail_lowercasesEmail_beforeLookup() {
        when(userRepository.findByEmail("john.doe@msg.group")).thenReturn(Optional.of(user));
        when(registrationRepository.countTotalRegistrationsPerUser(user.getId())).thenReturn(2);

        int result = registrationService.getRegistrationsPerUserByEmail("John.Doe@MSG.GROUP");

        assertThat(result).isEqualTo(2);
        verify(userRepository).findByEmail("john.doe@msg.group");
    }

    @Test
    void getRegistrationsPerUserByEmail_returnsZero_whenUserHasNoRegistrations() {
        when(userRepository.findByEmail("john.doe@msg.group")).thenReturn(Optional.of(user));
        when(registrationRepository.countTotalRegistrationsPerUser(user.getId())).thenReturn(0);

        int result = registrationService.getRegistrationsPerUserByEmail("john.doe@msg.group");

        assertThat(result).isZero();
    }

    @Test
    void getRegistrationsPerUserByEmail_throwsIllegalArgument_whenUserNotFound() {
        when(userRepository.findByEmail("missing.user@msg.group")).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> registrationService.getRegistrationsPerUserByEmail("missing.user@msg.group"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing.user@msg.group");

        verify(registrationRepository, never()).countTotalRegistrationsPerUser(user.getId());
    }
}

