package com.cluj1.eventapp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import com.cluj1.eventapp.dto.EventDetailsDto;
import com.cluj1.eventapp.model.EventDetails;
import com.cluj1.eventapp.repository.EventDetailsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventDetailsService {
    private final EventDetailsRepository eventDetailsRepository;

    public EventDetails getEventDetailsById(UUID id) {
        return eventDetailsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event details not found for id: " + id));
    }

    public EventDetailsDto getEventDetailsByEventId(UUID eventId) {
        EventDetails eventDetails = eventDetailsRepository.findByEventId(eventId)
                .orElseThrow(() -> new RuntimeException("Event details not found for event id: " + eventId));

        return EventDetailsDto.builder()
                .id(eventDetails.getId())
                .eventId(eventId)
                .description(eventDetails.getDescription())
                .foodProvided(eventDetails.getFoodProvided())
                .eventCode(eventDetails.getEventCode())
                .qrCodeContent(eventDetails.getQrCodeContent())
                .build();
    }

    public Optional<byte[]> getPosterByEventId(UUID eventId) {
        return eventDetailsRepository.findPosterByEventId(eventId)
                .filter(poster -> poster.length > 0);
    }
}
