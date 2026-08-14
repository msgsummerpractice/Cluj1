package com.cluj1.eventapp.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.dto.EventDto;
import com.cluj1.eventapp.mapper.EventMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    @Transactional(readOnly = true)
    public List<EventDto> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(eventMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventDto getEventById(UUID id) {
        return eventRepository.findById(id)
                .map(eventMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Event not found for id: " + id));
    }
}
