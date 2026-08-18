package com.cluj1.eventapp.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;

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
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.EventDetails;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.UserDetails;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.UserRepository;
import com.cluj1.eventapp.dto.EventDto;
import com.cluj1.eventapp.mapper.EventMapper;
import com.cluj1.eventapp.exception.InvalidEventOperationException;

import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor

@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventDetailsRepository eventDetailsRepository;
    private final EventMapper eventMapper;
    private final RegistrationRepository registrationRepository;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private final Random random = new Random();

    public int getUpcomingRegisteredEventsCountPerUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return eventRepository.countUpcomingEventsForUsers(OffsetDateTime.now(), user.getId());
    }

    @Transactional(readOnly = true)
    public List<EventDto> getAllEvents() {
        return eventRepository.findAll().stream()
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

    public List<EventDto> getEligibleEventsForCurrentUser() {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserDetails userDetails = user.getUserDetails();
        EventLocation userEventLocation = null;
        if (userDetails != null && userDetails.getLocation() != null) {
            try {
                userEventLocation = EventLocation.valueOf(userDetails.getLocation().name());
            } catch (IllegalArgumentException ignored) {
                // UserLocation.REMOTE has no EventLocation equivalent
            }
        }

        List<Event> eligibleEvents = userEventLocation != null
                ? eventRepository.findEligibleEvents(OffsetDateTime.now(), userEventLocation)
                : eventRepository.findAllLocationEligibleEvents(OffsetDateTime.now());

        return eligibleEvents.stream().map(event -> {
            EventDto dto = eventMapper.toDto(event);

            registrationRepository.findByUserIdAndEventId(user.getId(), event.getId())
                    .ifPresentOrElse(registration -> {
                        dto.setIsRegistered(true);
                        dto.setIsCheckedIn(registration.getAttendanceRecord() != null);
                    }, () -> {
                        dto.setIsRegistered(false);
                        dto.setIsCheckedIn(false);
                    });

            return dto;
        }).toList();
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
        EventDetails eventDetails = eventDetailsRepository.findByEvent(event);

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
        eventDetailsRepository.save(eventDetails);

        return new CheckInCodesDto(qrCodeConent, eventCode);

    }

    private String generateUniqueEventCode() {
        String code;
        boolean isUnique = false;

        do {
            code = String.format("%06d", random.nextInt(1000000));
            isUnique = !eventDetailsRepository.existsByEventCode(code);
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
}
