package com.cluj1.eventapp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.UserLocation;
import com.cluj1.eventapp.repository.UserDetailsRepository;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.UserDetails;
import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipientPoolService {
    private final UserDetailsRepository userDetailsRepository;

    public List<User> resolveRecipients(EventLocation eventLocation) {
        List<UserDetails> matchingDetails = eventLocation == EventLocation.ALL
                ? userDetailsRepository.findAll()
                : userDetailsRepository.findByLocation(toUserLocation(eventLocation));

        return matchingDetails.stream()
                .map(UserDetails::getUser)
                .toList();
    }

    private UserLocation toUserLocation(EventLocation eventLocation) {
        return switch (eventLocation) {
            case CLUJ -> UserLocation.CLUJ;
            case TIMISOARA -> UserLocation.TIMISOARA;
            case MURES -> UserLocation.MURES;
            case ALL -> throw new IllegalArgumentException(
                    "ALL should be handled before calling toUserLocation");
        };
    }
}
