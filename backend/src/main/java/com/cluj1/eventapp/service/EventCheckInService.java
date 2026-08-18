package com.cluj1.eventapp.service;

import com.cluj1.eventapp.dto.AttendanceRecordDto;
import com.cluj1.eventapp.mapper.AttendanceRecordMapper;
import com.cluj1.eventapp.dto.CheckInRequest;
import com.cluj1.eventapp.model.AttendanceRecord;
import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.EventStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final AttendanceRecordMapper attendanceRecordMapper;

    @Transactional
    public void processCheckIn(String userEmail, CheckInRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "checkin.error.user.notfound"));

        Event event = switch (request.getMethod()) {
            case QR -> findNotCompletedEventById(request.getEventId());
            case MANUAL -> findNotCompletedEventByCode(request.getEventCode());
        };

        if (event.getEventEndTime() != null && OffsetDateTime.now().isAfter(event.getEventEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkin.error.event.expired");
        }

        Registration registration = registrationRepository.findByUserIdAndEventId(user.getId(), event.getId())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkin.error.user.notregistered"));

        boolean alreadyCheckedIn = attendanceRecordRepository.existsByRegistrationId(registration.getId());
        if (alreadyCheckedIn) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "checkin.error.user.alreadycheckedin");
        }

        AttendanceRecord attendanceRecord = AttendanceRecord.builder()
                .registration(registration)
                .checkInMethod(request.getMethod())
                .checkInTime(OffsetDateTime.now())
                .build();
        attendanceRecordRepository.save(attendanceRecord);
    }

    private Event findNotCompletedEventById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkin.error.event.notfound"));
        ensureNotCompleted(event);
        return event;
    }

    private Event findNotCompletedEventByCode(String eventCode) {
        Event event = eventDetailsRepository.findEventByEventCode(eventCode.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkin.error.event.notfound"));
        ensureNotCompleted(event);
        return event;
    }

    private void ensureNotCompleted(Event event) {
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkin.error.event.completed");
        }
    }

    public List<AttendanceRecordDto> getRecentCheckins(UUID eventId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        return attendanceRecordRepository.findRecentByEventId(eventId, pageable)
                .stream()
                .map(attendanceRecordMapper::mapToDto)
                .toList();
    }
}