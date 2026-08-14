package com.cluj1.eventapp.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cluj1.eventapp.model.EventDetails;
import com.cluj1.eventapp.service.EventDetailsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/event-details")
@RequiredArgsConstructor
public class EventDetailsController {
    private final EventDetailsService eventDetailsService;

    @GetMapping("/{id}")
    public ResponseEntity<EventDetails> getEventDetailsById(@PathVariable UUID id) {
        EventDetails eventDetails = eventDetailsService.getEventDetailsById(id);
        return ResponseEntity.ok(eventDetails);
    }
}
