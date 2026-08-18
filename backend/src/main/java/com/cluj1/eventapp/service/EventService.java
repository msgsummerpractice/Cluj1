package com.cluj1.eventapp.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import com.cluj1.eventapp.dto.CheckInCodesDto;
import com.cluj1.eventapp.repository.EventDetailsRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.EventDetails;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.model.TransportationDetails;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

import com.cluj1.eventapp.dto.EventDto;
import com.cluj1.eventapp.dto.EventRegistrationDto;
import com.cluj1.eventapp.mapper.EventMapper;
import com.cluj1.eventapp.exception.InvalidEventOperationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final EventDetailsRepository eventDetailsReposity;
    private final EventMapper eventMapper;
    private final EventPublishMailService eventPublishMailService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private final Random random = new Random();

    public int getUpcomingRegisteredEventsCountPerUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return eventRepository.countUpcomingEventsForUsers(OffsetDateTime.now(), user.getId());
    }

    @Transactional(readOnly = true)
    public List<EventDto> getAllEvents() {
        return eventRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt", "id")).stream()
                .map(eventMapper::toDto)
                .toList();
    }

    @Transactional
    public EventDto createEvent(EventDto eventDto, MultipartFile poster) {
        validatePoster(poster);
        validateEventRules(eventDto);

        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new InvalidEventOperationException("Authenticated user not found in database."));

        Event event = Event.builder()
                .name(eventDto.getName())
                .type(eventDto.getType())
                .location(determineLocation(eventDto))
                .status(EventStatus.DRAFT)
                .eventStartDate(eventDto.getStartDate())
                .eventEndTime(eventDto.getEndDate())
                .createdBy(currentUser)
                .build();

        EventDetails details = EventDetails.builder()
                .event(event)
                .description(eventDto.getDescription())
                .foodProvided(determineFoodProvided(eventDto))
                .poster(extractBytes(poster))
                .build();

        event.setEventDetails(details);
        return eventMapper.toDto(eventRepository.save(event));
    }

    @Transactional
    public EventDto updateEventStatus(UUID id, EventStatus status) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        boolean justPublished = false;
        EventStatus currentStatus = event.getStatus();

        if (currentStatus == null) {
            throw new InvalidEventOperationException(
                    "Invalid status transition from null to " + status);
        }

        switch (currentStatus) {
            case DRAFT -> {
                if (status != EventStatus.PUBLISHED) {
                    throw new InvalidEventOperationException(
                            "Invalid status transition from " + currentStatus + " to " + status);
                }
                event.setStatus(EventStatus.PUBLISHED);
                justPublished = true;
                break;
            }
            case PUBLISHED -> {
                if (status != EventStatus.COMPLETED) {
                    throw new InvalidEventOperationException(
                            "Invalid status transition from " + currentStatus + " to " + status);
                }
                event.setStatus(EventStatus.COMPLETED);
                break;
            }
            default -> throw new InvalidEventOperationException(
                    "Invalid status transition from " + currentStatus + " to " + status);
        }

        Event saved = eventRepository.save(event);

        if (justPublished) {
            eventPublishMailService.notifyRecipients(saved);
        }

        return eventMapper.toDto(saved);
    }

    @Transactional
    public EventDto updateEvent(UUID id, EventDto eventDto, MultipartFile poster) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new InvalidEventOperationException("Event not found"));

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new InvalidEventOperationException("Only events in DRAFT status can be edited.");
        }

        validatePoster(poster);
        validateEventRules(eventDto);

        event.setName(eventDto.getName());
        event.setType(eventDto.getType());
        event.setLocation(determineLocation(eventDto));
        event.setEventStartDate(eventDto.getStartDate());
        event.setEventEndTime(eventDto.getEndDate());

        EventDetails details = event.getEventDetails();
        if (details == null) {
            details = EventDetails.builder().event(event).build();
            event.setEventDetails(details);
        }

        details.setDescription(eventDto.getDescription());
        details.setFoodProvided(determineFoodProvided(eventDto));

        if (poster != null && !poster.isEmpty()) {
            details.setPoster(extractBytes(poster));
        }

        return eventMapper.toDto(eventRepository.save(event));
    }

    private void validateEventRules(EventDto dto) {
        if (dto.getType() != EventType.INTERNAL && dto.getLocation() == null) {
            throw new InvalidEventOperationException("Location must be selected for LOCAL and EXTERNAL events.");
        }
    }

    private EventLocation determineLocation(EventDto dto) {
        return dto.getType() == EventType.INTERNAL ? EventLocation.ALL : dto.getLocation();
    }

    private Boolean determineFoodProvided(EventDto dto) {
        return dto.getType() != EventType.EXTERNAL && Boolean.TRUE.equals(dto.getFoodProvided());
    }

    private void validatePoster(MultipartFile poster) {
        if (poster != null && !poster.isEmpty()) {
            if (poster.getSize() > MAX_FILE_SIZE) {
                throw new InvalidEventOperationException("Poster file size exceeds 5MB limit.");
            }
            String contentType = poster.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
                throw new InvalidEventOperationException("Only JPEG and PNG formats are allowed for the poster.");
            }
        }
    }

    private byte[] extractBytes(MultipartFile file) {
        try {
            return (file != null && !file.isEmpty()) ? file.getBytes() : null;
        } catch (IOException e) {
            throw new InvalidEventOperationException("Failed to process poster upload");
        }
    }

    public EventDto getEventById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + id));

        return eventMapper.toDto(event);
    }

    @Transactional
    public CheckInCodesDto generateCheckInCodes(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        EventDetails eventDetails = eventDetailsReposity.findByEvent(event);

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot generate codes, Event is not published");
        }

        if (eventDetails.getEventCode() != null && eventDetails.getQrCodeContent() != null) {
            return new CheckInCodesDto(eventDetails.getQrCodeContent(), eventDetails.getEventCode());
        }

        String eventCode = generateUniqueEventCode();
        String qrCodeConent = generateQRCodeBase64(event);

        eventDetails.setEventCode(eventCode);
        eventDetails.setQrCodeContent(qrCodeConent);
        eventDetailsReposity.save(eventDetails);

        return new CheckInCodesDto(qrCodeConent, eventCode);

    }

    private String generateUniqueEventCode() {
        String code;
        boolean isUnique = false;

        do {
            code = String.format("%06d", random.nextInt(1000000));
            isUnique = !eventDetailsReposity.existsByEventCode(code);
        } while (!isUnique);

        return code;
    }

    private String generateQRCodeBase64(Event event) {
        try {

            String contentToEncode = String.format("EventID:%s|Name:%s", event.getId().toString(), event.getName());

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(contentToEncode, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);

        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR Code image", e);
        }
    }

    @Transactional
    public Registration registerUser(UUID eventId, String userEmail, EventRegistrationDto dto) {
        User user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (event.getRegistrationEndDate() != null && event.getRegistrationEndDate().isBefore(OffsetDateTime.now())) {
            throw new InvalidEventOperationException("Registration is closed for this event.");
        }

        if (eventRepository.existsByEventIdAndUserId(eventId, user.getId())) {
            return registrationRepository.findByEventIdAndUserId(eventId, user.getId())
                    .orElseThrow(() -> new IllegalStateException("Registration exists but could not be loaded."));
        }

        if (dto.getPhotoConsent() == null || !dto.getPhotoConsent()) {
            throw new InvalidEventOperationException("Photo consent is required for registration.");
        }

        if (event.getType() != EventType.EXTERNAL && (dto.getGdprConsent() == null || !dto.getGdprConsent())) {
            throw new InvalidEventOperationException("GDPR consent is required for this event type.");
        }

        if (event.getType() == EventType.INTERNAL) {
            validateInternalRegistration(dto);
        }

        Registration registration = Registration.builder()
                .user(user)
                .event(event)
                .gdprConsent(Boolean.TRUE.equals(dto.getGdprConsent()))
                .photoConsent(Boolean.TRUE.equals(dto.getPhotoConsent()))
                .foodPreference(dto.getFoodPreference())
                .transportationNeeded(dto.getTransportationNeeded() != null ? dto.getTransportationNeeded() : false)
                .accommodationNeeded(dto.getAccommodationNeeded() != null ? dto.getAccommodationNeeded() : false)
                .accommodationDays(dto.getAccommodationDays())
                .build();

        if (event.getType() == EventType.INTERNAL && Boolean.TRUE.equals(dto.getTransportationNeeded())) {
            TransportationDetails transportDetails = TransportationDetails.builder()
                    .registration(registration)
                    .driverName(dto.getDriverName())
                    .driverPhoneNumber(dto.getDriverPhone())
                    .build();
            registration.setTransportationDetails(transportDetails);
        }

        return registrationRepository.save(registration);
    }

    private void validateInternalRegistration(EventRegistrationDto dto) {
        if (Boolean.TRUE.equals(dto.getTransportationNeeded())) {
            if (dto.getDriverName() == null || dto.getDriverName().isBlank()) {
                throw new InvalidEventOperationException("Driver name is required when transportation is needed.");
            }

            if (dto.getDriverPhone() == null || dto.getDriverPhone().isBlank()) {
                throw new InvalidEventOperationException("Driver phone is required when transportation is needed.");
            }
        }

        if (Boolean.TRUE.equals(dto.getAccommodationNeeded())
                && (dto.getAccommodationDays() == null || dto.getAccommodationDays() < 1)) {
            throw new InvalidEventOperationException("Accommodation days must be provided when accommodation is needed.");
        }
    }

    public boolean isUserRegistered(UUID eventId, String userEmail) {
        User user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        return eventRepository.existsByEventIdAndUserId(eventId, user.getId());
    }
}