package com.cluj1.eventapp.service;

import com.cluj1.eventapp.dto.CheckInRequest;
import com.cluj1.eventapp.exception.InvalidEventOperationException;
import com.cluj1.eventapp.model.AttendanceRecord;
import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.EventStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.cluj1.eventapp.repository.AttendanceRecordRepository;
import com.cluj1.eventapp.repository.EventDetailsRepository;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventCheckInService {

    private final EventRepository eventRepository;
    private final EventDetailsRepository eventDetailsRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    @Transactional
    public void processCheckIn(String userEmail, CheckInRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "checkin.error.user.notfound"));

        Event event = findEventByCode(request.getCode());

        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new InvalidEventOperationException("checkin.error.event.completed");
        }

        if (event.getEventEndTime() != null && OffsetDateTime.now().isAfter(event.getEventEndTime())) {
            throw new InvalidEventOperationException("checkin.error.event.expired");
        }

        Registration registration = registrationRepository.findByUserIdAndEventId(user.getId(), event.getId())
                .orElseThrow(() -> new InvalidEventOperationException("checkin.error.user.notregistered"));

        boolean alreadyCheckedIn = attendanceRecordRepository.existsByRegistrationId(registration.getId());
        if (alreadyCheckedIn) {
            throw new InvalidEventOperationException("checkin.error.user.alreadycheckedin");
        }

        AttendanceRecord attendanceRecord = AttendanceRecord.builder()
                .registration(registration)
                .checkInMethod(request.getMethod())
                .checkInTime(OffsetDateTime.now())
                .build();
        attendanceRecordRepository.save(attendanceRecord);
    }

    private Event findEventByCode(String code) {
        try {
            UUID eventId = UUID.fromString(code);
            return eventRepository.findById(eventId)
                    .orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "checkin.error.event.notfound"));
        } catch (IllegalArgumentException e) {
            return eventDetailsRepository.findEventByEventCode(code)
                    .orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "checkin.error.event.notfound"));
        }
    }
}
