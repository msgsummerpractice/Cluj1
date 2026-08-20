package com.cluj1.eventapp.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.EventDetails;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.model.TransportationDetails;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.UserDetails;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;
import com.cluj1.eventapp.model.enums.FoodPreference;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

import com.cluj1.eventapp.dto.EventDto;
import com.cluj1.eventapp.dto.EventRegistrationDto;
import com.cluj1.eventapp.dto.EventStatisticsDto;
import com.cluj1.eventapp.dto.ParticipantDetailDto;
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
    private final EventDetailsRepository eventDetailsRepository;
    private final RegistrationRepository registrationRepository;
    private final EventMapper eventMapper;
    private final EventPublishMailService eventPublishMailService;
    private final RecipientPoolService recipientPoolService;

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

    private List<Event> fetchEligibleEventsForUser(UserDetails userDetails) {
        OffsetDateTime now = OffsetDateTime.now();

        if (userDetails == null || userDetails.getLocation() == null) {
            return eventRepository.findAllLocationEligibleEvents(now);
        }

        try {
            EventLocation userEventLocation = EventLocation.valueOf(userDetails.getLocation().name());
            return eventRepository.findEligibleEvents(now, userEventLocation, EventStatus.PUBLISHED);
        } catch (IllegalArgumentException e) {
            return eventRepository.findAllLocationEligibleEvents(now);
        }
    }

    /**
     * Returns all events the currently authenticated participant is eligible to
     * register for.
     * <p>
     * An event is eligible when:
     * <ul>
     * <li>its status is {@code PUBLISHED}</li>
     * <li>its {@code registrationEndDate} has not yet passed</li>
     * <li>its location is {@code ALL}, or matches the participant's own
     * location</li>
     * </ul>
     * Users whose location is {@code REMOTE} or who have no profile details on
     * record
     * are shown only {@code ALL}-location events.
     * <p>
     * Each returned {@link EventDto} is enriched with {@code isRegistered} and
     * {@code isCheckedIn} flags reflecting the caller's current participation
     * state.
     *
     * @return list of eligible events, never {@code null}
     */
    public List<EventDto> getEligibleEventsForCurrentUser() {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserDetails userDetails = user.getUserDetails();

        List<Event> eligibleEvents = fetchEligibleEventsForUser(userDetails);

        Set<UUID> eventIds = eligibleEvents.stream()
                .map(Event::getId)
                .collect(Collectors.toSet());

        Map<UUID, Registration> registrationsByEventId = registrationRepository
                .findByUserIdAndEventIdIn(user.getId(), eventIds)
                .stream()
                .collect(Collectors.toMap(r -> r.getEvent().getId(), r -> r));

        List<EventDto> eligibleEventDtos = eligibleEvents.stream().map(event -> {
            EventDto dto = eventMapper.toDto(event);
            Registration registration = registrationsByEventId.get(event.getId());
            if (registration != null) {
                dto.setIsRegistered(true);
                dto.setIsCheckedIn(registration.getAttendanceRecord() != null);
            } else {
                dto.setIsRegistered(false);
                dto.setIsCheckedIn(false);
            }
            return dto;
        }).toList();

        return eligibleEventDtos;
    }

    public EventDto getEventById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + id));

        return eventMapper.toDto(event);
    }

    /**
     * Returns the check-in codes (QR content and event code) for the given event.
     * Only available for {@code PUBLISHED} events that have had their codes
     * generated.
     *
     * @param eventId the event identifier
     * @return {@link CheckInCodesDto} containing the QR code and 6-character event
     *         code
     * @throws InvalidEventOperationException if the event is not published or codes
     *                                        have not been generated
     */
    public CheckInCodesDto getCheckInDetails(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new InvalidEventOperationException("Event not found"));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new InvalidEventOperationException("checkin.error.event.notpublished");
        }

        EventDetails details = event.getEventDetails();
        if (details == null || details.getEventCode() == null || details.getQrCodeContent() == null) {
            throw new InvalidEventOperationException("checkin.error.codes.notgenerated");
        }

        return new CheckInCodesDto(details.getQrCodeContent(), details.getEventCode());
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

            String contentToEncode = event.getId().toString();

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
            throw new InvalidEventOperationException("User is already registered for this event.");
        }

        EventDetails eventDetails = eventDetailsRepository.findByEvent(event);
        boolean foodProvided = eventDetails != null && Boolean.TRUE.equals(eventDetails.getFoodProvided());

        FoodPreference foodPreference = FoodPreference.NONE;

        if (event.getType() != EventType.EXTERNAL && foodProvided) {
            if (dto.getFoodPreference() != null) {
                foodPreference = dto.getFoodPreference();
            }
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
                .foodPreference(foodPreference)
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
            throw new InvalidEventOperationException(
                    "Accommodation days must be provided when accommodation is needed.");
        }
    }

    public boolean isUserRegistered(UUID eventId, String userEmail) {
        User user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return eventRepository.existsByEventIdAndUserId(eventId, user.getId());
    }

    public EventStatisticsDto getEventStatistics(UUID eventId) {
    Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found"));

    int invitedCount = 0;
    try {
        invitedCount = recipientPoolService.getRecipientsCountForEvent(event.getLocation());
    } catch (Exception e) {
        log.warn("Could not fetch recipient pool count for event {}", eventId);
    }
    
    List<Registration> registrations = registrationRepository.findByEventId(eventId);
    int registrationCount = registrations.size();
    
    long participantCount = registrations.stream()
            .filter(r -> r.getAttendanceRecord() != null)
            .count();

    Map<FoodPreference, Long> foodCounts = registrations.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.getFoodPreference() != null ? r.getFoodPreference() : FoodPreference.NONE,
                        java.util.stream.Collectors.counting()
                ));

    Map<String, Double> foodPercentages = new java.util.HashMap<>();
    if (registrationCount > 0) {
        foodCounts.forEach((pref, count) -> 
            foodPercentages.put(pref.name(), (count * 100.0) / registrationCount)
        );
    }

    long accommodationCount = registrations.stream().filter(r -> Boolean.TRUE.equals(r.getAccommodationNeeded())).count();
    long transportCount = registrations.stream().filter(r -> Boolean.TRUE.equals(r.getTransportationNeeded())).count();
    long photoConsentCount = registrations.stream().filter(r -> Boolean.TRUE.equals(r.getPhotoConsent())).count();

    double accommodationPercentage = registrationCount > 0 ? (accommodationCount * 100.0) / registrationCount : 0.0;
    double transportPercentage = registrationCount > 0 ? (transportCount * 100.0) / registrationCount : 0.0;
    double photoConsentPercentage = registrationCount > 0 ? (photoConsentCount * 100.0) / registrationCount : 0.0;

    Map<String, Long> timeDistribution = new java.util.LinkedHashMap<>();
    registrations.forEach(r -> {
        if (r.getRegistrationDate() != null) {
            String dayKey = r.getRegistrationDate().toLocalDate().toString();
            timeDistribution.put(dayKey, timeDistribution.getOrDefault(dayKey, 0L) + 1);
        }
    });

    List<ParticipantDetailDto> participantDetails = registrations.stream().map(r -> 
    ParticipantDetailDto.builder()
        .name(r.getUser().getUserDetails() != null ? 
              r.getUser().getUserDetails().getFirstName() + " " + r.getUser().getUserDetails().getLastName() : "Unknown")
        .email(r.getUser().getEmail())
        .status(r.getAttendanceRecord() != null ? "CHECKED_IN" : "REGISTERED")
        .checkInTime(r.getAttendanceRecord() != null ? r.getAttendanceRecord().getCheckInTime().toString() : "-")
        .build()
    ).toList();

    return EventStatisticsDto.builder()
            .invitedCount(invitedCount)
            .registrationCount(registrationCount)
            .participantCount(participantCount)
            .registrationTimeDistribution(timeDistribution)
            .foodPreferencePercentages(foodPercentages)
            .accommodationPercentage(accommodationPercentage)
            .transportPercentage(transportPercentage)
            .photoConsentPercentage(photoConsentPercentage)
            .participants(participantDetails)
            .build();
}
}