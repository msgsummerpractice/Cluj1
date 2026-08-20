package com.cluj1.eventapp.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cluj1.eventapp.dto.CheckInCodesDto;
import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.service.AttendanceExcelGeneratorService;
import com.cluj1.eventapp.mapper.EventMapper;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.service.EventService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

import com.cluj1.eventapp.dto.AttendanceRecordDto;
import com.cluj1.eventapp.dto.EventDetailsDto;
import com.cluj1.eventapp.dto.EventDto;
import com.cluj1.eventapp.dto.EventRegistrationDto;

import com.cluj1.eventapp.model.enums.EventStatus;

import org.springframework.web.bind.annotation.*;

import com.cluj1.eventapp.service.EventCheckInService;
import com.cluj1.eventapp.service.EventDetailsService;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final EventDetailsService eventDetailsService;
    private final EventCheckInService eventCheckInService;

    private final EventMapper eventMapper;

    @GetMapping("/countRegistrationPerUser")
    public ResponseEntity<Integer> getRegistrationCountPerUser(Principal principal) {
        String email = principal.getName();
        return ResponseEntity.ok(eventService.getUpcomingRegisteredEventsCountPerUserByEmail(email));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER', 'HR_USER', 'ADMIN')")
    public ResponseEntity<List<EventDto>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PARTICIPANT', 'MARKETING_ORGANIZER', 'HR_USER', 'ADMIN')")
    public ResponseEntity<EventDto> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

	@GetMapping("/{id}/details")
	@PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER', 'HR_USER', 'ADMIN', 'PARTICIPANT')")
	public ResponseEntity<EventDetailsDto> getEventDetails(@PathVariable UUID id) {
		return ResponseEntity.ok(eventDetailsService.getEventDetailsByEventId(id));
	}

    @GetMapping("/{id}/checkin")
    @PreAuthorize("hasAnyAuthority('PARTICIPANT', 'MARKETING_ORGANIZER', 'HR_USER', 'ADMIN')")
    public ResponseEntity<CheckInCodesDto> getEventCheckInDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getCheckInDetails(id));
    }

    @GetMapping("/{id}/poster")
    @PreAuthorize("hasAnyAuthority('PARTICIPANT', 'MARKETING_ORGANIZER', 'HR_USER', 'ADMIN')")
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

    /**
     * Returns the list of events the authenticated participant is eligible to
     * register for.
     * Eligibility is determined by the service based on event status, registration
     * deadline,
     * and location matching. Each event in the response includes
     * {@code isRegistered} and
     * {@code isCheckedIn} flags reflecting the caller's current participation
     * state.
     *
     * @return 200 OK with the eligible event list
     */
    @GetMapping("/eligible")
    @PreAuthorize("hasAnyAuthority('PARTICIPANT', 'MARKETING_ORGANIZER', 'HR_USER', 'ADMIN')")
    public ResponseEntity<List<EventDto>> getEligibleEvents() {
        return ResponseEntity.ok(eventService.getEligibleEventsForCurrentUser());
    }

    @PostMapping("/{eventId}")
    @PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER', 'HR_USER', 'ADMIN', 'USER', 'PARTICIPANT')")
    public ResponseEntity<Map<String, String>> registerForEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody EventRegistrationDto requestDto,
            Principal principal) {

        String email = principal.getName();
        eventService.registerUser(eventId, email, requestDto);

        return ResponseEntity.ok(Map.of("message", "Successfully registered for the event."));
    }

    @GetMapping("/{eventId}/check")
    public ResponseEntity<Boolean> checkIfRegistered(
            @PathVariable UUID eventId,
            Principal principal) {

        String userEmail = principal.getName();
        boolean isRegistered = eventService.isUserRegistered(eventId, userEmail);

        return ResponseEntity.ok(isRegistered);
    }

    @GetMapping("/{id}/checkins/recent")
    @PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER', 'HR_USER', 'ADMIN')")
    public ResponseEntity<List<AttendanceRecordDto>> getRecentCheckins(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(eventCheckInService.getRecentCheckins(id, limit));
    }

    @PatchMapping("/{id}/status/{status}")
    @PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER')")
    public ResponseEntity<EventDto> updateEventStatus(
            @PathVariable UUID id,
            @PathVariable EventStatus status) {
        return ResponseEntity.ok(eventService.updateEventStatus(id, status));
    }

    @PostMapping("/{id}/checkin-codes")
    @PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER')")
    public ResponseEntity<CheckInCodesDto> generateCheckInCodes(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.generateCheckInCodes(id));
    }

    @PatchMapping("{eventId}/manage")
    @PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER', 'HR_USER', 'ADMIN', 'USER', 'PARTICIPANT')")
    public ResponseEntity<?> updateRegistration(@PathVariable UUID eventId, @Valid @RequestBody EventRegistrationDto newRequestDto, Principal principal) {
        String email = principal.getName();

        Registration updatedRegistration = eventService.updateRegistration(eventId, email, newRequestDto);

        if(updatedRegistration == null) {
            return ResponseEntity.ok(Map.of("message", "Registration automatically removed due to GDPR consent"));
        }
        return ResponseEntity.ok(Map.of("message", "Successfully updated registration"));

    }
    @DeleteMapping("/{eventId}/manage")
    @PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER', 'HR_USER', 'ADMIN', 'USER', 'PARTICIPANT')")
    public ResponseEntity<String> deleteRegistration(@PathVariable UUID eventId, Principal principal) {
        String email = principal.getName();

        try{
            eventService.deleteRegistration(eventId, email);
            return ResponseEntity.ok("Successfully deleted registration");
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting registration");
        }

    }
    @GetMapping("/{eventId}/registration")
    @PreAuthorize("hasAnyAuthority('MARKETING_ORGANIZER', 'HR_USER', 'ADMIN', 'USER', 'PARTICIPANT')")
    public ResponseEntity<EventRegistrationDto> getRegistrationDetails(@PathVariable UUID eventId, Principal principal) {
        String email = principal.getName();

        Registration registration = eventService.getRegistration(eventId, email);

        return ResponseEntity.ok(eventMapper.toEventRegistrationDto(registration));
    }
}
