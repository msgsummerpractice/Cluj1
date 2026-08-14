package com.cluj1.eventapp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.UserDetails;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.model.enums.UserLocation;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {

        User adminUser = User.builder()
                .email("test.admin@msg.group")
                .passwordHash("hash")
                .role(Role.valueOf("ADMIN"))
                .isActive(true)
                .build();

        UserDetails adminDetails = UserDetails.builder()
                .user(adminUser)
                .firstName("TestAdmin")
                .lastName("TestSuprem")
                .location(UserLocation.valueOf("CLUJ"))
                .build();
        adminUser.setUserDetails(adminDetails);

        User participantUser = User.builder()
                .email("test.participant@msg.group")
                .passwordHash("hash")
                .role(Role.valueOf("PARTICIPANT"))
                .isActive(true)
                .build();

        UserDetails participantDetails = UserDetails.builder()
                .user(participantUser)
                .firstName("TestAndrei")
                .lastName("TestPopescu")
                .location(UserLocation.valueOf("CLUJ"))
                .build();
        participantUser.setUserDetails(participantDetails);

        User hrUser = User.builder()
                .email("test.hr@msg.group")
                .passwordHash("hash")
                .role(Role.valueOf("PARTICIPANT"))
                .isActive(true)
                .build();

        UserDetails hrDetails = UserDetails.builder()
                .user(hrUser)
                .firstName("TestElena")
                .lastName("TestResurseUmane")
                .location(UserLocation.valueOf("MURES"))
                .build();
        hrUser.setUserDetails(hrDetails);

        userRepository.saveAll(List.of(adminUser, participantUser, hrUser));
    }

    @Test
    void searchUsers_returnAll_whenSearchTermIsNull() {
        Page<User> results = userRepository.searchUsers(null, PageRequest.of(0, 100));
        assertThat(results.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void searchUsers_returnAll_whenSearchTermIsEmpty() {
        Page<User> results = userRepository.searchUsers("", PageRequest.of(0, 100));
        assertThat(results.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void searchUsers_matchByFirstNameCaseInsensitive() {
        Page<User> results = userRepository.searchUsers("testadmin", PageRequest.of(0, 100));
        assertThat(results.getContent()).anyMatch(user -> user.getEmail().equals("test.admin@msg.group"));
    }

    @Test
    void searchUsers_matchByLastNameCaseInsensitive() {
        Page<User> results = userRepository.searchUsers("TESTPOPESCU", PageRequest.of(0, 100));
        assertThat(results.getContent()).anyMatch(user -> user.getUserDetails().getFirstName().equals("TestAndrei"));
    }

    @Test
    void searchUsers_matchByEmailPartial() {
        Page<User> results = userRepository.searchUsers("test.participant", PageRequest.of(0, 100));
        assertThat(results.getContent()).anyMatch(user -> user.getUserDetails().getLastName().equals("TestPopescu"));
    }

    @Test
    void searchUsers_matchByRole() {
        Page<User> results = userRepository.searchUsers("ADMIN", PageRequest.of(0, 100));
        assertThat(results.getContent()).anyMatch(user -> user.getEmail().equals("test.admin@msg.group"));
    }

    @Test
    void searchUsers_returnEmpty_whenNoMatch() {
        Page<User> results = userRepository.searchUsers("DateCareNuExistaNiciodata123", PageRequest.of(0, 100));
        assertThat(results.getContent()).isEmpty();
    }
}