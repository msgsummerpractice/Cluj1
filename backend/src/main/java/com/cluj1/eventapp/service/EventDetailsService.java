package com.cluj1.eventapp.service;

import org.springframework.stereotype.Service;
import java.util.UUID;

import com.cluj1.eventapp.model.EventDetails;
import com.cluj1.eventapp.repository.EventDetailsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventDetailsService {
    private final EventDetailsRepository eventDetailsRepository;

    public EventDetails getEventDetailsById(UUID id) {
        return eventDetailsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event details not found for id: " + id));
    }
}
