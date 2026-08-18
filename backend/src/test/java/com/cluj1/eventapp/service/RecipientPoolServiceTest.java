package com.cluj1.eventapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.UserDetails;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.UserLocation;
import com.cluj1.eventapp.repository.UserDetailsRepository;

@ExtendWith(MockitoExtension.class)
class RecipientPoolServiceTest {

    @Mock
    private UserDetailsRepository userDetailsRepository;

    @InjectMocks
    private RecipientPoolService recipientPoolService;

    @Test
    void resolveRecipientsReturnsAllUsersWhenEventLocationIsAll() {
        User firstUser = new User();
        User secondUser = new User();
        UserDetails firstDetails = userDetails(firstUser, UserLocation.CLUJ);
        UserDetails secondDetails = userDetails(secondUser, UserLocation.MURES);
        when(userDetailsRepository.findAll()).thenReturn(List.of(firstDetails, secondDetails));

        List<User> result = recipientPoolService.resolveRecipients(EventLocation.ALL);

        assertEquals(List.of(firstUser, secondUser), result);
        verify(userDetailsRepository).findAll();
        verifyNoMoreInteractions(userDetailsRepository);
    }

    @Test
    void resolveRecipientsReturnsUsersForClujEvent() {
        User user = new User();
        when(userDetailsRepository.findByLocation(UserLocation.CLUJ))
                .thenReturn(List.of(userDetails(user, UserLocation.CLUJ)));

        List<User> result = recipientPoolService.resolveRecipients(EventLocation.CLUJ);

        assertEquals(1, result.size());
        assertSame(user, result.get(0));
        verify(userDetailsRepository).findByLocation(UserLocation.CLUJ);
        verifyNoMoreInteractions(userDetailsRepository);
    }

    @Test
    void resolveRecipientsReturnsUsersForTimisoaraEvent() {
        User user = new User();
        when(userDetailsRepository.findByLocation(UserLocation.TIMISOARA))
                .thenReturn(List.of(userDetails(user, UserLocation.TIMISOARA)));

        List<User> result = recipientPoolService.resolveRecipients(EventLocation.TIMISOARA);

        assertEquals(List.of(user), result);
        verify(userDetailsRepository).findByLocation(UserLocation.TIMISOARA);
        verifyNoMoreInteractions(userDetailsRepository);
    }

    @Test
    void resolveRecipientsReturnsUsersForMuresEvent() {
        User user = new User();
        when(userDetailsRepository.findByLocation(UserLocation.MURES))
                .thenReturn(List.of(userDetails(user, UserLocation.MURES)));

        List<User> result = recipientPoolService.resolveRecipients(EventLocation.MURES);

        assertEquals(List.of(user), result);
        verify(userDetailsRepository).findByLocation(UserLocation.MURES);
        verifyNoMoreInteractions(userDetailsRepository);
    }

    private UserDetails userDetails(User user, UserLocation location) {
        return UserDetails.builder()
                .user(user)
                .location(location)
                .build();
    }

}
