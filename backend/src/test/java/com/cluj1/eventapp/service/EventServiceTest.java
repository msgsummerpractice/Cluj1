package com.cluj1.eventapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cluj1.eventapp.dto.EventDto;
import com.cluj1.eventapp.exception.InvalidEventOperationException;
import com.cluj1.eventapp.mapper.EventMapper;
import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;
import com.cluj1.eventapp.model.AttendanceRecord;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.model.UserDetails;
import com.cluj1.eventapp.model.enums.UserLocation;
import com.cluj1.eventapp.repository.EventDetailsRepository;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

	@Mock
	private EventRepository eventRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private EventMapper eventMapper;

	@Mock
	private EventDetailsRepository eventDetailsRepository;

	@Mock
	private RegistrationRepository registrationRepository;

	@InjectMocks
	private EventService eventService;

	private EventDto requestDto;
	private User mockUser;

	@BeforeEach
	void setUp() {
		requestDto = EventDto.builder()
				.name("Tech Meetup")
				.type(EventType.LOCAL)
				.location(EventLocation.CLUJ)
				.foodProvided(true)
				.build();

		mockUser = User.builder().id(UUID.randomUUID()).email("test@msg.group").build();
	}

	private void mockSecurityContext() {
		Authentication authentication = mock(Authentication.class);
		SecurityContext securityContext = mock(SecurityContext.class);
		lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
		lenient().when(authentication.getName()).thenReturn("test@msg.group");
		SecurityContextHolder.setContext(securityContext);
		lenient().when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
	}

	@Test
	void getAllEventsReturnsMappedEventsInRepositoryOrder() {
		Event event1 = Event.builder().id(UUID.randomUUID()).name("Event 1").build();
		Event event2 = Event.builder().id(UUID.randomUUID()).name("Event 2").build();
		when(eventRepository.findAll(any(Sort.class))).thenReturn(List.of(event1, event2));
		when(eventMapper.toDto(event1)).thenReturn(EventDto.builder().id(event1.getId()).name("Event 1").build());
		when(eventMapper.toDto(event2)).thenReturn(EventDto.builder().id(event2.getId()).name("Event 2").build());

		List<EventDto> result = eventService.getAllEvents();

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getName()).isEqualTo("Event 1");
		assertThat(result.get(1).getName()).isEqualTo("Event 2");
		verify(eventRepository).findAll(any(Sort.class));
		verify(eventMapper).toDto(event1);
		verify(eventMapper).toDto(event2);
	}

	@Test
	void getAllEventsWhenRepositoryIsEmptyReturnsEmptyList() {
		when(eventRepository.findAll(any(Sort.class))).thenReturn(List.of());

		List<EventDto> result = eventService.getAllEvents();

		assertThat(result).isEmpty();
		verify(eventRepository).findAll(any(Sort.class));
		verify(eventMapper, never()).toDto(any());
	}

	@Test
	void testCreateEvent_local_success() {
		mockSecurityContext();
		when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArguments()[0]);
		when(eventMapper.toDto(any(Event.class))).thenReturn(requestDto);

		EventDto result = eventService.createEvent(requestDto, null);

		assertNotNull(result);
		verify(eventRepository).save(argThat(e -> e.getStatus() == EventStatus.DRAFT &&
				e.getLocation() == EventLocation.CLUJ &&
				e.getEventDetails().getFoodProvided() == true &&
				e.getCreatedBy().equals(mockUser)));
	}

	@Test
	void testCreateEvent_internal_forcesLocationAll() {
		mockSecurityContext();
		requestDto.setType(EventType.INTERNAL);
		requestDto.setLocation(EventLocation.CLUJ);

		when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArguments()[0]);

		eventService.createEvent(requestDto, null);

		verify(eventRepository).save(argThat(e -> e.getLocation() == EventLocation.ALL));
	}

	@Test
	void testCreateEvent_external_clearsFoodProvided() {
		mockSecurityContext();
		requestDto.setType(EventType.EXTERNAL);

		when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArguments()[0]);

		eventService.createEvent(requestDto, null);

		verify(eventRepository).save(argThat(e -> e.getEventDetails().getFoodProvided() == false));
	}

	@Test
	void testUpdateEvent_notInDraft_throwsException() {
		UUID id = UUID.randomUUID();
		Event publishedEvent = Event.builder().status(EventStatus.PUBLISHED).build();
		when(eventRepository.findById(id)).thenReturn(Optional.of(publishedEvent));

		assertThrows(InvalidEventOperationException.class, () -> eventService.updateEvent(id, requestDto, null));
	}

	@Test
	void testInvalidPosterSize_throwsException() {
		byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
		MockMultipartFile file = new MockMultipartFile("poster", "test.jpg", "image/jpeg", largeContent);

		assertThrows(InvalidEventOperationException.class, () -> eventService.createEvent(requestDto, file));
	}

	private void mockUserWithLocation(UserLocation location) {
		UserDetails userDetails = UserDetails.builder().location(location).build();
		mockUser.setUserDetails(userDetails);
		mockSecurityContext();
	}

	@Test
	void getEligibleEventsReturnsEmptyListWhenRepositoryReturnsNoEvents() {
		mockUserWithLocation(UserLocation.CLUJ);
		when(eventRepository.findEligibleEvents(any(), eq(EventLocation.CLUJ), eq(EventStatus.PUBLISHED)))
				.thenReturn(List.of());

		List<EventDto> result = eventService.getEligibleEventsForCurrentUser();

		assertThat(result).isEmpty();
	}

	@Test
	void getEligibleEventsSetsFalseRegistrationStatusWhenUserNotRegistered() {
		mockUserWithLocation(UserLocation.CLUJ);
		Event event = Event.builder().id(UUID.randomUUID()).name("ClujFest").build();
		EventDto dto = EventDto.builder().id(event.getId()).name(event.getName()).build();

		when(eventRepository.findEligibleEvents(any(), eq(EventLocation.CLUJ), eq(EventStatus.PUBLISHED)))
				.thenReturn(List.of(event));
		when(eventMapper.toDto(event)).thenReturn(dto);
		when(registrationRepository.findByUserIdAndEventId(mockUser.getId(), event.getId()))
				.thenReturn(Optional.empty());

		List<EventDto> result = eventService.getEligibleEventsForCurrentUser();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getIsRegistered()).isFalse();
		assertThat(result.get(0).getIsCheckedIn()).isFalse();
	}

	@Test
	void getEligibleEventsSetsIsRegisteredTrueWhenUserHasRegistration() {
		mockUserWithLocation(UserLocation.CLUJ);
		Event event = Event.builder().id(UUID.randomUUID()).name("ClujFest").build();
		EventDto dto = EventDto.builder().id(event.getId()).name(event.getName()).build();
		Registration registration = Registration.builder().id(UUID.randomUUID()).build();

		when(eventRepository.findEligibleEvents(any(), eq(EventLocation.CLUJ), eq(EventStatus.PUBLISHED)))
				.thenReturn(List.of(event));
		when(eventMapper.toDto(event)).thenReturn(dto);
		when(registrationRepository.findByUserIdAndEventId(mockUser.getId(), event.getId()))
				.thenReturn(Optional.of(registration));

		List<EventDto> result = eventService.getEligibleEventsForCurrentUser();

		assertThat(result.get(0).getIsRegistered()).isTrue();
		assertThat(result.get(0).getIsCheckedIn()).isFalse();
	}

	@Test
	void getEligibleEventsSetsIsCheckedInTrueWhenAttendanceRecordExists() {
		mockUserWithLocation(UserLocation.CLUJ);
		Event event = Event.builder().id(UUID.randomUUID()).name("ClujFest").build();
		EventDto dto = EventDto.builder().id(event.getId()).name(event.getName()).build();
		AttendanceRecord attendanceRecord = AttendanceRecord.builder().id(UUID.randomUUID()).build();
		Registration registration = Registration.builder()
				.id(UUID.randomUUID())
				.attendanceRecord(attendanceRecord)
				.build();

		when(eventRepository.findEligibleEvents(any(), eq(EventLocation.CLUJ), eq(EventStatus.PUBLISHED)))
				.thenReturn(List.of(event));
		when(eventMapper.toDto(event)).thenReturn(dto);
		when(registrationRepository.findByUserIdAndEventId(mockUser.getId(), event.getId()))
				.thenReturn(Optional.of(registration));

		List<EventDto> result = eventService.getEligibleEventsForCurrentUser();

		assertThat(result.get(0).getIsRegistered()).isTrue();
		assertThat(result.get(0).getIsCheckedIn()).isTrue();
	}

	@Test
	void getEligibleEventsUsesAllLocationQueryForRemoteUser() {
		mockUserWithLocation(UserLocation.REMOTE);
		when(eventRepository.findAllLocationEligibleEvents(any())).thenReturn(List.of());

		eventService.getEligibleEventsForCurrentUser();

		verify(eventRepository).findAllLocationEligibleEvents(any());
		verify(eventRepository, never()).findEligibleEvents(any(), any(), any());
	}

	@Test
	void getEligibleEventsUsesAllLocationQueryWhenUserHasNoDetails() {
		mockSecurityContext();
		when(eventRepository.findAllLocationEligibleEvents(any())).thenReturn(List.of());

		eventService.getEligibleEventsForCurrentUser();

		verify(eventRepository).findAllLocationEligibleEvents(any());
		verify(eventRepository, never()).findEligibleEvents(any(), any(), any());
	}
}
