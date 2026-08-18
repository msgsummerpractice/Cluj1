package com.cluj1.eventapp.service;

import com.cluj1.eventapp.dto.CheckInRequest;
import com.cluj1.eventapp.model.AttendanceRecord;
import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.CheckInMethod;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.repository.AttendanceRecordRepository;
import com.cluj1.eventapp.repository.EventDetailsRepository;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventCheckInServiceTest {

        @Mock
        private EventRepository eventRepository;
        @Mock
        private EventDetailsRepository eventDetailsRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private RegistrationRepository registrationRepository;
        @Mock
        private AttendanceRecordRepository attendanceRecordRepository;

        @InjectMocks
        private EventCheckInService checkInService;

        private User user;
        private Event event;
        private Registration registration;
        private final String USER_EMAIL = "john.doe@msg.group";

        @BeforeEach
        void setUp() {
                user = User.builder()
                                .id(UUID.randomUUID())
                                .email(USER_EMAIL)
                                .role(Role.PARTICIPANT)
                                .isActive(true)
                                .build();

                event = Event.builder()
                                .id(UUID.randomUUID())
                                .name("Tech Summit")
                                .status(EventStatus.PUBLISHED)
                                .build();

                registration = Registration.builder()
                                .id(UUID.randomUUID())
                                .user(user)
                                .event(event)
                                .build();
        }

        private CheckInRequest buildRequest(String code, CheckInMethod method) {
                CheckInRequest req = new CheckInRequest();
                req.setCode(code);
                req.setMethod(method);
                return req;
        }

        @Test
        void processCheckIn_withValidUuid_savesAttendanceRecord() {
                CheckInRequest request = buildRequest(event.getId().toString(), CheckInMethod.QR);

                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
                when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
                when(registrationRepository.findByUserIdAndEventId(user.getId(), event.getId()))
                                .thenReturn(Optional.of(registration));
                when(attendanceRecordRepository.existsByRegistrationId(registration.getId())).thenReturn(false);

                checkInService.processCheckIn(USER_EMAIL, request);

                ArgumentCaptor<AttendanceRecord> captor = ArgumentCaptor.forClass(AttendanceRecord.class);
                verify(attendanceRecordRepository).save(captor.capture());
                assertThat(captor.getValue().getCheckInMethod()).isEqualTo(CheckInMethod.QR);
                assertThat(captor.getValue().getRegistration()).isEqualTo(registration);
        }

        @Test
        void processCheckIn_withValidEventCode_savesAttendanceRecord() {
                CheckInRequest request = buildRequest("ABC123", CheckInMethod.MANUAL);

                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
                when(eventDetailsRepository.findEventByEventCode("ABC123")).thenReturn(Optional.of(event));
                when(registrationRepository.findByUserIdAndEventId(user.getId(), event.getId()))
                                .thenReturn(Optional.of(registration));
                when(attendanceRecordRepository.existsByRegistrationId(registration.getId())).thenReturn(false);

                checkInService.processCheckIn(USER_EMAIL, request);

                ArgumentCaptor<AttendanceRecord> captor = ArgumentCaptor.forClass(AttendanceRecord.class);
                verify(attendanceRecordRepository).save(captor.capture());
                assertThat(captor.getValue().getCheckInMethod()).isEqualTo(CheckInMethod.MANUAL);
        }

        @Test
        void processCheckIn_userNotFound_throwsNotFound() {
                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> checkInService.processCheckIn(USER_EMAIL,
                                buildRequest(event.getId().toString(), CheckInMethod.QR)))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("checkin.error.user.notfound");

                verify(attendanceRecordRepository, never()).save(any());
        }

        @Test
        void processCheckIn_eventNotFoundByUuid_throwsNotFound() {
                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
                when(eventRepository.findById(any())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> checkInService.processCheckIn(USER_EMAIL,
                                buildRequest(event.getId().toString(), CheckInMethod.QR)))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("checkin.error.event.notfound");

                verify(attendanceRecordRepository, never()).save(any());
        }

        @Test
        void processCheckIn_eventNotFoundByCode_throwsNotFound() {
                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
                when(eventDetailsRepository.findEventByEventCode("XYZ999")).thenReturn(Optional.empty());

                assertThatThrownBy(
                                () -> checkInService.processCheckIn(USER_EMAIL,
                                                buildRequest("XYZ999", CheckInMethod.MANUAL)))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("checkin.error.event.notfound");

                verify(attendanceRecordRepository, never()).save(any());
        }

        @Test
        void processCheckIn_eventCompleted_throwsInvalidOperation() {
                event.setStatus(EventStatus.COMPLETED);

                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
                when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

                assertThatThrownBy(() -> checkInService.processCheckIn(USER_EMAIL,
                                buildRequest(event.getId().toString(), CheckInMethod.QR)))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("checkin.error.event.completed")
                                .satisfies(exception -> assertThat(
                                                ((ResponseStatusException) exception).getStatusCode().value())
                                                .isEqualTo(400));

                verify(attendanceRecordRepository, never()).save(any());
        }

        @Test
        void processCheckIn_eventEndTimeInPast_throwsInvalidOperation() {
                event.setEventEndTime(OffsetDateTime.now().minusHours(1));

                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
                when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

                assertThatThrownBy(() -> checkInService.processCheckIn(USER_EMAIL,
                                buildRequest(event.getId().toString(), CheckInMethod.QR)))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("checkin.error.event.expired")
                                .satisfies(exception -> assertThat(
                                                ((ResponseStatusException) exception).getStatusCode().value())
                                                .isEqualTo(400));

                verify(attendanceRecordRepository, never()).save(any());
        }

        @Test
        void processCheckIn_eventEndTimeInFuture_doesNotThrow() {
                event.setEventEndTime(OffsetDateTime.now().plusHours(1));

                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
                when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
                when(registrationRepository.findByUserIdAndEventId(user.getId(), event.getId()))
                                .thenReturn(Optional.of(registration));
                when(attendanceRecordRepository.existsByRegistrationId(registration.getId())).thenReturn(false);

                checkInService.processCheckIn(USER_EMAIL, buildRequest(event.getId().toString(), CheckInMethod.QR));

                verify(attendanceRecordRepository).save(any());
        }

        @Test
        void processCheckIn_nullEventEndTime_doesNotThrow() {
                event.setEventEndTime(null);

                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
                when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
                when(registrationRepository.findByUserIdAndEventId(user.getId(), event.getId()))
                                .thenReturn(Optional.of(registration));
                when(attendanceRecordRepository.existsByRegistrationId(registration.getId())).thenReturn(false);

                checkInService.processCheckIn(USER_EMAIL, buildRequest(event.getId().toString(), CheckInMethod.QR));

                verify(attendanceRecordRepository).save(any());
        }

        @Test
        void processCheckIn_userNotRegistered_throwsInvalidOperation() {
                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
                when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
                when(registrationRepository.findByUserIdAndEventId(user.getId(), event.getId()))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> checkInService.processCheckIn(USER_EMAIL,
                                buildRequest(event.getId().toString(), CheckInMethod.QR)))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("checkin.error.user.notregistered")
                                .satisfies(exception -> assertThat(
                                                ((ResponseStatusException) exception).getStatusCode().value())
                                                .isEqualTo(400));

                verify(attendanceRecordRepository, never()).save(any());
        }

        @Test
        void processCheckIn_alreadyCheckedIn_throwsInvalidOperation() {
                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
                when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
                when(registrationRepository.findByUserIdAndEventId(user.getId(), event.getId()))
                                .thenReturn(Optional.of(registration));
                when(attendanceRecordRepository.existsByRegistrationId(registration.getId())).thenReturn(true);

                assertThatThrownBy(() -> checkInService.processCheckIn(USER_EMAIL,
                                buildRequest(event.getId().toString(), CheckInMethod.QR)))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("checkin.error.user.alreadycheckedin")
                                .satisfies(exception -> assertThat(
                                                ((ResponseStatusException) exception).getStatusCode().value())
                                                .isEqualTo(409));

                verify(attendanceRecordRepository, never()).save(any());
        }
}
