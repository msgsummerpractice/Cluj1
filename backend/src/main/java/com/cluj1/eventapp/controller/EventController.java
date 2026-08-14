package com.cluj1.eventapp.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import com.cluj1.eventapp.dto.EventDetailsDto;
import com.cluj1.eventapp.dto.EventDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cluj1.eventapp.service.EventDetailsService;
import com.cluj1.eventapp.service.EventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

	private final EventService eventService;
	private final EventDetailsService eventDetailsService;

	@GetMapping
	@PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER', 'HR_USER', 'ADMIN')")
	public ResponseEntity<List<EventDto>> getAllEvents() {
		return ResponseEntity.ok(eventService.getAllEvents());
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER', 'HR_USER', 'ADMIN')")
	public ResponseEntity<EventDto> getEventById(@PathVariable UUID id) {
		return ResponseEntity.ok(eventService.getEventById(id));
	}

	@GetMapping("/{id}/details")
	@PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER', 'HR_USER', 'ADMIN')")
	public ResponseEntity<EventDetailsDto> getEventDetails(@PathVariable UUID id) {
		return ResponseEntity.ok(eventDetailsService.getEventDetailsByEventId(id));
	}

}
