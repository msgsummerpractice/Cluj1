package com.cluj1.eventapp.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cluj1.eventapp.dto.UserDTO;
import com.cluj1.eventapp.dto.UserProfileDto;
import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.UserDetails;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.model.enums.UserLocation;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UserMapper(passwordEncoder);
    }

    @Test
    void mapToEntity_setsAllFields_andEncodesPassword_andWiresUserDetails() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail("john.doe@msg.group");
        dto.setPassword("Password1!");
        dto.setUserLocation(UserLocation.CLUJ);
        when(passwordEncoder.encode("Password1!")).thenReturn("HASHED");

        User user = mapper.mapToEntity(dto);

        assertThat(user.getEmail()).isEqualTo("john.doe@msg.group");
        assertThat(user.getPasswordHash()).isEqualTo("HASHED");
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUserDetails()).isNotNull();
        assertThat(user.getUserDetails().getFirstName()).isEqualTo("John");
        assertThat(user.getUserDetails().getLastName()).isEqualTo("Doe");
        assertThat(user.getUserDetails().getLocation()).isEqualTo(UserLocation.CLUJ);
        // The bidirectional wiring
        assertThat(user.getUserDetails().getUser()).isSameAs(user);
    }

    @Test
    void mapToDTO_mapsAllFields_whenUserDetailsPresent() {
        UUID id = UUID.randomUUID();
        UserDetails details = UserDetails.builder()
                .firstName("Jane")
                .lastName("Smith")
                .location(UserLocation.TIMISOARA)
                .build();
        User user = User.builder()
                .id(id)
                .email("jane.smith@msg.group")
                .role(Role.HR_USER)
                .isActive(true)
                .userDetails(details)
                .build();

        UserDTO userDto = mapper.mapToDTO(user);

        assertThat(userDto.getId()).isEqualTo(id);
        assertThat(userDto.getEmail()).isEqualTo("jane.smith@msg.group");
        assertThat(userDto.getRole()).isEqualTo(Role.HR_USER);
        assertThat(userDto.getIsActive()).isTrue();
        assertThat(userDto.getFirstName()).isEqualTo("Jane");
        assertThat(userDto.getLastName()).isEqualTo("Smith");
        assertThat(userDto.getLocation()).isEqualTo(UserLocation.TIMISOARA);
    }

    @Test
    void mapToDTO_leavesDetailFieldsNull_whenUserDetailsMissing() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("no.details@msg.group")
                .role(Role.PARTICIPANT)
                .isActive(false)
                .build();

        UserDTO userDto = mapper.mapToDTO(user);

        assertThat(userDto.getFirstName()).isNull();
        assertThat(userDto.getLastName()).isNull();
        assertThat(userDto.getLocation()).isNull();
        assertThat(userDto.getIsActive()).isFalse();
    }

    @Test
    void mapUserToUserProfileDto_mapsAllFields_whenUserDetailsPresent() {
        byte[] pic = { 1, 2, 3 };
        UserDetails details = UserDetails.builder()
                .firstName("Jane")
                .lastName("Smith")
                .location(UserLocation.MURES)
                .profilePicture(pic)
                .build();
        User user = User.builder()
                .email("jane.smith@msg.group")
                .role(Role.ADMIN)
                .userDetails(details)
                .build();

        UserProfileDto dto = mapper.mapUserToUserProfileDto(user);

        assertThat(dto.getFirstName()).isEqualTo("Jane");
        assertThat(dto.getLastName()).isEqualTo("Smith");
        assertThat(dto.getEmail()).isEqualTo("jane.smith@msg.group");
        assertThat(dto.getRole()).isEqualTo(Role.ADMIN);
        assertThat(dto.getUserLocation()).isEqualTo(UserLocation.MURES);
        assertThat(dto.getProfilePicture()).isEqualTo(pic);
    }

    @Test
    void mapUserToUserProfileDto_leavesDetailFieldsNull_whenUserDetailsMissing() {
        User user = User.builder()
                .email("no.details@msg.group")
                .role(Role.PARTICIPANT)
                .build();

        UserProfileDto dto = mapper.mapUserToUserProfileDto(user);

        assertThat(dto.getEmail()).isEqualTo("no.details@msg.group");
        assertThat(dto.getRole()).isEqualTo(Role.PARTICIPANT);
        assertThat(dto.getFirstName()).isNull();
        assertThat(dto.getLastName()).isNull();
        assertThat(dto.getUserLocation()).isNull();
        assertThat(dto.getProfilePicture()).isNull();
    }
}

