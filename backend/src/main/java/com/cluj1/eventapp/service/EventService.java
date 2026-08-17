package com.cluj1.eventapp.service;

import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public int getUpcomingRegisteredEventsCountPerUserByEmail(String email){
        User user = userRepository.findByEmail(email).orElseThrow(()-> new IllegalArgumentException("User not found"));
        return eventRepository.countUpcomingEventsForUsers(OffsetDateTime.now(), user.getId());
    }
}
