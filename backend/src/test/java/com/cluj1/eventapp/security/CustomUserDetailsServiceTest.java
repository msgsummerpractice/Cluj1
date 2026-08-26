package com.cluj1.eventapp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("john.doe@msg.group")
                .passwordHash("hash")
                .role(Role.MARKETING_ORGANIZER)
                .isActive(true)
                .build();
    }

    @Test
    void loadUserByUsername_returnsUserDetails_withRoleAsAuthority() {
        when(userRepository.findByEmail("john.doe@msg.group")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("john.doe@msg.group");

        assertThat(result.getUsername()).isEqualTo("john.doe@msg.group");
        assertThat(result.getPassword()).isEqualTo("hash");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("MARKETING_ORGANIZER");
    }

    @Test
    void loadUserByUsername_returnsDisabledUser_whenIsActiveFalse() {
        user.setIsActive(false);
        when(userRepository.findByEmail("john.doe@msg.group")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("john.doe@msg.group");

        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_throwsUsernameNotFoundException_whenEmailUnknown() {
        when(userRepository.findByEmail("missing.user@msg.group")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing.user@msg.group"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void loadUserById_returnsUserDetails_whenIdKnown() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserById(user.getId().toString());

        assertThat(result.getUsername()).isEqualTo("john.doe@msg.group");
    }

    @Test
    void loadUserById_throwsUsernameNotFoundException_whenIdUnknown() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserById(missing.toString()))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserById_throwsIllegalArgumentException_whenIdIsNotUuid() {
        assertThatThrownBy(() -> service.loadUserById("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

