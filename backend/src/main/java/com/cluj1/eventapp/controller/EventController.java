package com.cluj1.eventapp.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
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

	@GetMapping("/{id}/poster")
	@PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER', 'HR_USER', 'ADMIN')")
	public ResponseEntity<byte[]> getEventPoster(@PathVariable UUID id) {
		return eventDetailsService.getPosterByEventId(id)
				.map(poster -> ResponseEntity.ok()
						.contentType(detectImageType(poster))
						.body(poster))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	private static MediaType detectImageType(byte[] poster) {
		if (poster.length >= 3 && (poster[0] & 0xFF) == 0xFF && (poster[1] & 0xFF) == 0xD8) {
			return MediaType.IMAGE_JPEG;
		}
		if (poster.length >= 4 && (poster[0] & 0xFF) == 0x47 && poster[1] == 'I' && poster[2] == 'F') {
			return MediaType.IMAGE_GIF;
		}
		if (poster.length >= 12 && poster[0] == 'R' && poster[1] == 'I' && poster[2] == 'F' && poster[3] == 'F'
				&& poster[8] == 'W' && poster[9] == 'E' && poster[10] == 'B' && poster[11] == 'P') {
			return MediaType.valueOf("image/webp");
		}
		return MediaType.IMAGE_PNG;
	}

}
