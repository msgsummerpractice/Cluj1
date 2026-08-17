package com.cluj1.eventapp.controller;

import java.util.List;
import java.util.UUID;

import com.cluj1.eventapp.service.EventService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import com.cluj1.eventapp.dto.EventDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventController {

	private final EventService eventService;

    @GetMapping("/countRegistrationPerUser")
    public ResponseEntity<Integer> getRegistrationCountPerUser(Principal principal){
        String email = principal.getName();
        return ResponseEntity.ok(eventService.getUpcomingRegisteredEventsCountPerUserByEmail(email));
    }

	@GetMapping
	@PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER', 'HR_USER', 'ADMIN')")
	public ResponseEntity<List<EventDto>> getAllEvents() {
		return ResponseEntity.ok(eventService.getAllEvents());
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER')")
	public ResponseEntity<EventDto> createEvent(
			@RequestPart("event") EventDto eventDto,
			@RequestPart(value = "poster", required = false) MultipartFile poster) {
		return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(eventDto, poster));
	}

	@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER')")
	public ResponseEntity<EventDto> updateEvent(
			@PathVariable UUID id,
			@RequestPart("event") EventDto eventDto,
			@RequestPart(value = "poster", required = false) MultipartFile poster) {
		return ResponseEntity.ok(eventService.updateEvent(id, eventDto, poster));
	}
}
