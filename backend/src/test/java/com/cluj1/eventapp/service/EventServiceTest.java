package com.cluj1.eventapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

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
import com.cluj1.eventapp.dto.EventRegistrationDto;
import com.cluj1.eventapp.dto.EventStatisticsDto;
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

	@Mock
	private EventPublishMailService eventPublishMailService;

	@Mock
	private RecipientPoolService recipientPoolService;

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
		byte[] largeContent = new byte[6 * 1024 * 1024];
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
		lenient().when(eventMapper.toDto(event)).thenReturn(dto);
		when(registrationRepository.findByUserIdAndEventIdIn(eq(mockUser.getId()), any()))
				.thenReturn(List.of());

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
		Registration registration = Registration.builder().id(UUID.randomUUID()).event(event).build();

		when(eventRepository.findEligibleEvents(any(), eq(EventLocation.CLUJ), eq(EventStatus.PUBLISHED)))
				.thenReturn(List.of(event));
		when(eventMapper.toDto(event)).thenReturn(dto);
		when(registrationRepository.findByUserIdAndEventIdIn(eq(mockUser.getId()), any()))
				.thenReturn(List.of(registration));

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
				.event(event)
				.attendanceRecord(attendanceRecord)
				.build();

		when(eventRepository.findEligibleEvents(any(), eq(EventLocation.CLUJ), eq(EventStatus.PUBLISHED)))
				.thenReturn(List.of(event));
		when(eventMapper.toDto(event)).thenReturn(dto);
		when(registrationRepository.findByUserIdAndEventIdIn(eq(mockUser.getId()), any()))
				.thenReturn(List.of(registration));

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

	// ---------- getUpcomingRegisteredEventsCountPerUserByEmail ----------

	@Test
	void getUpcomingRegisteredEventsCountPerUserByEmail_returnsCount() {
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(eventRepository.countUpcomingEventsForUsers(any(), eq(mockUser.getId()))).thenReturn(3);

		int count = eventService.getUpcomingRegisteredEventsCountPerUserByEmail("test@msg.group");

		assertThat(count).isEqualTo(3);
	}

	@Test
	void getUpcomingRegisteredEventsCountPerUserByEmail_throwsWhenUserNotFound() {
		when(userRepository.findByEmail("missing@msg.group")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class,
				() -> eventService.getUpcomingRegisteredEventsCountPerUserByEmail("missing@msg.group"));
	}


	@Test
	void getEventById_returnsDto_whenEventFound_andNoAuthenticatedUser() {
		SecurityContextHolder.clearContext();
		UUID id = UUID.randomUUID();
		Event event = Event.builder().id(id).name("E").build();
		EventDto dto = EventDto.builder().id(id).name("E").build();
		when(eventRepository.findById(id)).thenReturn(Optional.of(event));
		when(eventMapper.toDto(event)).thenReturn(dto);

		EventDto result = eventService.getEventById(id);

		assertThat(result.getIsRegistered()).isFalse();
		assertThat(result.getIsCheckedIn()).isFalse();
	}

	@Test
	void getEventById_throwsIllegalArgument_whenEventNotFound() {
		UUID id = UUID.randomUUID();
		when(eventRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.getEventById(id))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(id.toString());
	}

	// ---------- getCheckInDetails ----------

	@Test
	void getCheckInDetails_returnsCodes_whenPublishedAndCodesGenerated() {
		UUID id = UUID.randomUUID();
		com.cluj1.eventapp.model.EventDetails details = com.cluj1.eventapp.model.EventDetails.builder()
				.eventCode("ABC123")
				.qrCodeContent("data:image/png;base64,AAA")
				.build();
		Event event = Event.builder().id(id).status(EventStatus.PUBLISHED).eventDetails(details).build();
		when(eventRepository.findById(id)).thenReturn(Optional.of(event));

		com.cluj1.eventapp.dto.CheckInCodesDto result = eventService.getCheckInDetails(id);

		assertThat(result.getEventCode()).isEqualTo("ABC123");
		assertThat(result.getQrCodeContent()).isEqualTo("data:image/png;base64,AAA");
	}

	@Test
	void getCheckInDetails_throws_whenEventNotFound() {
		UUID id = UUID.randomUUID();
		when(eventRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.getCheckInDetails(id))
				.isInstanceOf(InvalidEventOperationException.class);
	}

	@Test
	void getCheckInDetails_throws_whenEventNotPublished() {
		UUID id = UUID.randomUUID();
		Event event = Event.builder().id(id).status(EventStatus.DRAFT).build();
		when(eventRepository.findById(id)).thenReturn(Optional.of(event));

		assertThatThrownBy(() -> eventService.getCheckInDetails(id))
				.isInstanceOf(InvalidEventOperationException.class)
				.hasMessageContaining("notpublished");
	}

	@Test
	void getCheckInDetails_throws_whenCodesNotGenerated() {
		UUID id = UUID.randomUUID();
		com.cluj1.eventapp.model.EventDetails details = com.cluj1.eventapp.model.EventDetails.builder().build();
		Event event = Event.builder().id(id).status(EventStatus.PUBLISHED).eventDetails(details).build();
		when(eventRepository.findById(id)).thenReturn(Optional.of(event));

		assertThatThrownBy(() -> eventService.getCheckInDetails(id))
				.isInstanceOf(InvalidEventOperationException.class)
				.hasMessageContaining("notgenerated");
	}

	// ---------- updateEventStatus ----------

	@Test
	void updateEventStatus_draftToPublished_succeeds_andNotifiesRecipients() {
		UUID id = UUID.randomUUID();
		Event event = Event.builder().id(id).status(EventStatus.DRAFT).build();
		when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(event));
		when(eventRepository.save(event)).thenReturn(event);
		when(eventMapper.toDto(event)).thenReturn(EventDto.builder().status(EventStatus.PUBLISHED).build());

		EventDto result = eventService.updateEventStatus(id, EventStatus.PUBLISHED);

		assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
		assertThat(result.getStatus()).isEqualTo(EventStatus.PUBLISHED);
		verify(eventPublishMailService).notifyRecipients(event);
	}

	@Test
	void updateEventStatus_publishedToCompleted_succeeds() {
		UUID id = UUID.randomUUID();
		Event event = Event.builder().id(id).status(EventStatus.PUBLISHED).build();
		when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(event));
		when(eventRepository.save(event)).thenReturn(event);
		when(eventMapper.toDto(event)).thenReturn(EventDto.builder().status(EventStatus.COMPLETED).build());

		eventService.updateEventStatus(id, EventStatus.COMPLETED);

		assertThat(event.getStatus()).isEqualTo(EventStatus.COMPLETED);
		verify(eventPublishMailService, never()).notifyRecipients(any());
	}

	@Test
	void updateEventStatus_draftToInvalidTarget_throws() {
		UUID id = UUID.randomUUID();
		Event event = Event.builder().id(id).status(EventStatus.DRAFT).build();
		when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(event));

		assertThatThrownBy(() -> eventService.updateEventStatus(id, EventStatus.COMPLETED))
				.isInstanceOf(InvalidEventOperationException.class);
	}

	@Test
	void updateEventStatus_draftToPublished_throwsWhenEventAlreadyEnded() {
		UUID id = UUID.randomUUID();
		Event event = Event.builder().id(id).status(EventStatus.DRAFT)
				.eventStartDate(OffsetDateTime.now().minusDays(2))
				.eventEndTime(OffsetDateTime.now().minusDays(1))
				.build();
		when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(event));

		assertThatThrownBy(() -> eventService.updateEventStatus(id, EventStatus.PUBLISHED))
				.isInstanceOf(InvalidEventOperationException.class)
				.hasMessageContaining("already ended");
	}

	@Test
	void updateEventStatus_draftToPublished_throwsWhenEndDateNotAfterStart() {
		UUID id = UUID.randomUUID();
		OffsetDateTime start = OffsetDateTime.now().plusDays(1);
		Event event = Event.builder().id(id).status(EventStatus.DRAFT)
				.eventStartDate(start)
				.eventEndTime(start)
				.build();
		when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(event));

		assertThatThrownBy(() -> eventService.updateEventStatus(id, EventStatus.PUBLISHED))
				.isInstanceOf(InvalidEventOperationException.class)
				.hasMessageContaining("end date is not after start date");
	}

	@Test
	void updateEventStatus_publishedToInvalidTarget_throws() {
		UUID id = UUID.randomUUID();
		Event event = Event.builder().id(id).status(EventStatus.PUBLISHED).build();
		when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(event));

		assertThatThrownBy(() -> eventService.updateEventStatus(id, EventStatus.DRAFT))
				.isInstanceOf(InvalidEventOperationException.class);
	}

	@Test
	void updateEventStatus_fromCompleted_throws() {
		UUID id = UUID.randomUUID();
		Event event = Event.builder().id(id).status(EventStatus.COMPLETED).build();
		when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(event));

		assertThatThrownBy(() -> eventService.updateEventStatus(id, EventStatus.PUBLISHED))
				.isInstanceOf(InvalidEventOperationException.class);
	}

	@Test
	void updateEventStatus_throwsEntityNotFound_whenEventMissing() {
		UUID id = UUID.randomUUID();
		when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.updateEventStatus(id, EventStatus.PUBLISHED))
				.isInstanceOf(EntityNotFoundException.class);
	}

	// ---------- registerUser ----------

	private Event publishedEvent(EventType type) {
		return Event.builder()
				.id(UUID.randomUUID())
				.status(EventStatus.PUBLISHED)
				.type(type)
				.registrationEndDate(OffsetDateTime.now().plusDays(1))
				.build();
	}

	@Test
	void registerUser_throwsIllegalArgument_whenUserNotFound() {
		UUID eventId = UUID.randomUUID();
		when(userRepository.findByEmail("missing@msg.group")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.registerUser(eventId, "missing@msg.group",
				EventRegistrationDto.builder().gdprConsent(true).photoConsent(true).build()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void registerUser_throwsIllegalArgument_whenEventNotFound() {
		UUID eventId = UUID.randomUUID();
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.registerUser(eventId, "test@msg.group",
				EventRegistrationDto.builder().gdprConsent(true).photoConsent(true).build()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void registerUser_throwsInvalidOperation_whenRegistrationClosed() {
		UUID eventId = UUID.randomUUID();
		Event event = Event.builder()
				.id(eventId)
				.registrationEndDate(OffsetDateTime.now().minusHours(1))
				.type(EventType.LOCAL)
				.build();
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

		assertThatThrownBy(() -> eventService.registerUser(eventId, "test@msg.group",
				EventRegistrationDto.builder().gdprConsent(true).photoConsent(true).build()))
				.isInstanceOf(InvalidEventOperationException.class)
				.hasMessageContaining("Registration is closed");
	}

	@Test
	void registerUser_throwsInvalidOperation_whenAlreadyRegistered() {
		Event event = publishedEvent(EventType.LOCAL);
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(eventRepository.existsByEventIdAndUserId(event.getId(), mockUser.getId())).thenReturn(true);

		assertThatThrownBy(() -> eventService.registerUser(event.getId(), "test@msg.group",
				EventRegistrationDto.builder().gdprConsent(true).photoConsent(true).build()))
				.isInstanceOf(InvalidEventOperationException.class)
				.hasMessageContaining("already registered");
	}

	@Test
	void registerUser_throwsInvalidOperation_whenGdprMissingForInternalEvent() {
		Event event = publishedEvent(EventType.INTERNAL);
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(eventRepository.existsByEventIdAndUserId(event.getId(), mockUser.getId())).thenReturn(false);
		when(eventDetailsRepository.findByEvent(event)).thenReturn(
				com.cluj1.eventapp.model.EventDetails.builder().foodProvided(false).build());

		assertThatThrownBy(() -> eventService.registerUser(event.getId(), "test@msg.group",
				EventRegistrationDto.builder().gdprConsent(false).photoConsent(true).build()))
				.isInstanceOf(InvalidEventOperationException.class)
				.hasMessageContaining("GDPR");
	}

	@Test
	void registerUser_succeeds_forExternalEvent_ignoringGdprAndFood() {
		Event event = publishedEvent(EventType.EXTERNAL);
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(eventRepository.existsByEventIdAndUserId(event.getId(), mockUser.getId())).thenReturn(false);
		when(eventDetailsRepository.findByEvent(event)).thenReturn(
				com.cluj1.eventapp.model.EventDetails.builder().foodProvided(false).build());
		when(registrationRepository.save(any(Registration.class))).thenAnswer(i -> i.getArgument(0));

		Registration reg = eventService.registerUser(event.getId(), "test@msg.group",
				EventRegistrationDto.builder().gdprConsent(false).photoConsent(false).build());

		assertThat(reg.getFoodPreference()).isEqualTo(com.cluj1.eventapp.model.enums.FoodPreference.NONE);
		assertThat(reg.getTransportationDetails()).isNull();
	}

	@Test
	void registerUser_internalWithTransport_savesTransportationDetails() {
		Event event = publishedEvent(EventType.INTERNAL);
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(eventRepository.existsByEventIdAndUserId(event.getId(), mockUser.getId())).thenReturn(false);
		when(eventDetailsRepository.findByEvent(event)).thenReturn(
				com.cluj1.eventapp.model.EventDetails.builder().foodProvided(true).build());
		when(registrationRepository.save(any(Registration.class))).thenAnswer(i -> i.getArgument(0));

		EventRegistrationDto dto = EventRegistrationDto.builder()
				.gdprConsent(true)
				.photoConsent(true)
				.foodPreference(com.cluj1.eventapp.model.enums.FoodPreference.VEGAN)
				.transportationNeeded(true)
				.driverName("Jane")
				.driverPhone("+40123456789")
				.build();

		Registration reg = eventService.registerUser(event.getId(), "test@msg.group", dto);

		assertThat(reg.getTransportationDetails()).isNotNull();
		assertThat(reg.getTransportationDetails().getDriverName()).isEqualTo("Jane");
		assertThat(reg.getFoodPreference()).isEqualTo(com.cluj1.eventapp.model.enums.FoodPreference.VEGAN);
	}

	@Test
	void registerUser_internalWithTransport_throwsWhenDriverNameMissing() {
		Event event = publishedEvent(EventType.INTERNAL);
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(eventRepository.existsByEventIdAndUserId(event.getId(), mockUser.getId())).thenReturn(false);
		when(eventDetailsRepository.findByEvent(event)).thenReturn(
				com.cluj1.eventapp.model.EventDetails.builder().foodProvided(false).build());

		EventRegistrationDto dto = EventRegistrationDto.builder()
				.gdprConsent(true).photoConsent(true)
				.transportationNeeded(true).driverName("").driverPhone("+40123456789")
				.build();

		assertThatThrownBy(() -> eventService.registerUser(event.getId(), "test@msg.group", dto))
				.isInstanceOf(InvalidEventOperationException.class)
				.hasMessageContaining("Driver name");
	}

	@Test
	void registerUser_internalWithTransport_throwsWhenDriverPhoneMissing() {
		Event event = publishedEvent(EventType.INTERNAL);
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(eventRepository.existsByEventIdAndUserId(event.getId(), mockUser.getId())).thenReturn(false);
		when(eventDetailsRepository.findByEvent(event)).thenReturn(
				com.cluj1.eventapp.model.EventDetails.builder().foodProvided(false).build());

		EventRegistrationDto dto = EventRegistrationDto.builder()
				.gdprConsent(true).photoConsent(true)
				.transportationNeeded(true).driverName("Jane").driverPhone("  ")
				.build();

		assertThatThrownBy(() -> eventService.registerUser(event.getId(), "test@msg.group", dto))
				.isInstanceOf(InvalidEventOperationException.class)
				.hasMessageContaining("Driver phone");
	}

	@Test
	void registerUser_internalWithAccommodation_throwsWhenDaysMissing() {
		Event event = publishedEvent(EventType.INTERNAL);
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(eventRepository.existsByEventIdAndUserId(event.getId(), mockUser.getId())).thenReturn(false);
		when(eventDetailsRepository.findByEvent(event)).thenReturn(
				com.cluj1.eventapp.model.EventDetails.builder().foodProvided(false).build());

		EventRegistrationDto dto = EventRegistrationDto.builder()
				.gdprConsent(true).photoConsent(true)
				.accommodationNeeded(true).accommodationDays(0)
				.build();

		assertThatThrownBy(() -> eventService.registerUser(event.getId(), "test@msg.group", dto))
				.isInstanceOf(InvalidEventOperationException.class)
				.hasMessageContaining("Accommodation days");
	}


	@Test
	void isUserRegistered_returnsTrue_whenRegistrationExists() {
		UUID eventId = UUID.randomUUID();
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(eventRepository.existsByEventIdAndUserId(eventId, mockUser.getId())).thenReturn(true);

		assertThat(eventService.isUserRegistered(eventId, "test@msg.group")).isTrue();
	}

	@Test
	void isUserRegistered_returnsFalse_whenNoRegistration() {
		UUID eventId = UUID.randomUUID();
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(eventRepository.existsByEventIdAndUserId(eventId, mockUser.getId())).thenReturn(false);

		assertThat(eventService.isUserRegistered(eventId, "test@msg.group")).isFalse();
	}

	@Test
	void isUserRegistered_throws_whenUserNotFound() {
		when(userRepository.findByEmail("missing@msg.group")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.isUserRegistered(UUID.randomUUID(), "missing@msg.group"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// ---------- deleteRegistration ----------

	@Test
	void deleteRegistration_deletes_whenRegistered() {
		UUID eventId = UUID.randomUUID();
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(eventRepository.existsByEventIdAndUserId(eventId, mockUser.getId())).thenReturn(true);

		eventService.deleteRegistration(eventId, "test@msg.group");

		verify(registrationRepository).deleteRegistrationByUserEmailAndEventId("test@msg.group", eventId);
	}

	@Test
	void deleteRegistration_throws_whenNotRegistered() {
		UUID eventId = UUID.randomUUID();
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(eventRepository.existsByEventIdAndUserId(eventId, mockUser.getId())).thenReturn(false);

		assertThatThrownBy(() -> eventService.deleteRegistration(eventId, "test@msg.group"))
				.isInstanceOf(InvalidEventOperationException.class);
		verify(registrationRepository, never()).deleteRegistrationByUserEmailAndEventId(any(), any());
	}


	@Test
	void getRegistrationByEmail_returns_whenExists() {
		UUID eventId = UUID.randomUUID();
		Registration reg = Registration.builder().id(UUID.randomUUID()).build();
		when(registrationRepository.findByEventIdAndUserEmail(eventId, "test@msg.group"))
				.thenReturn(Optional.of(reg));

		Registration result = eventService.getRegistration(eventId, "test@msg.group");

		assertThat(result).isEqualTo(reg);
	}

	@Test
	void getRegistrationByEmail_throws_whenMissing() {
		UUID eventId = UUID.randomUUID();
		when(registrationRepository.findByEventIdAndUserEmail(eventId, "test@msg.group"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.getRegistration(eventId, "test@msg.group"))
				.isInstanceOf(InvalidEventOperationException.class)
				.hasMessageContaining("Registration not found");
	}

	@Test
	void updateRegistration_removesRegistration_whenGdprRevokedForInternal() {
		UUID eventId = UUID.randomUUID();
		Event event = Event.builder().id(eventId).type(EventType.INTERNAL).build();
		Registration reg = Registration.builder().id(UUID.randomUUID()).user(mockUser).event(event).build();
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(registrationRepository.findByEventIdAndUserId(eventId, mockUser.getId())).thenReturn(Optional.of(reg));
		when(eventRepository.existsByEventIdAndUserId(eventId, mockUser.getId())).thenReturn(true);

		EventRegistrationDto dto = EventRegistrationDto.builder().gdprConsent(false).photoConsent(true).build();

		Registration result = eventService.updateRegistration(eventId, "test@msg.group", dto);

		assertThat(result).isNull();
		verify(registrationRepository).deleteRegistrationByUserEmailAndEventId("test@msg.group", eventId);
	}

	@Test
	void updateRegistration_savesRegistration_whenExternalEvent() {
		UUID eventId = UUID.randomUUID();
		Event event = Event.builder().id(eventId).type(EventType.EXTERNAL).build();
		Registration reg = Registration.builder().id(UUID.randomUUID()).user(mockUser).event(event).build();
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(registrationRepository.findByEventIdAndUserId(eventId, mockUser.getId())).thenReturn(Optional.of(reg));
		when(registrationRepository.save(reg)).thenReturn(reg);

		EventRegistrationDto dto = EventRegistrationDto.builder().gdprConsent(false).photoConsent(true).build();

		Registration result = eventService.updateRegistration(eventId, "test@msg.group", dto);

		assertThat(result).isEqualTo(reg);
		assertThat(reg.getFoodPreference()).isEqualTo(com.cluj1.eventapp.model.enums.FoodPreference.NONE);
	}

	@Test
	void updateRegistration_removesExistingTransportation_whenTransportNoLongerNeeded() {
		UUID eventId = UUID.randomUUID();
		Event event = Event.builder().id(eventId).type(EventType.INTERNAL).build();
		com.cluj1.eventapp.model.TransportationDetails td = com.cluj1.eventapp.model.TransportationDetails.builder()
				.driverName("X").driverPhoneNumber("1").build();
		Registration reg = Registration.builder().id(UUID.randomUUID()).user(mockUser).event(event)
				.transportationDetails(td).build();
		when(userRepository.findByEmail("test@msg.group")).thenReturn(Optional.of(mockUser));
		when(registrationRepository.findByEventIdAndUserId(eventId, mockUser.getId())).thenReturn(Optional.of(reg));
		when(eventDetailsRepository.findByEvent(event)).thenReturn(
				com.cluj1.eventapp.model.EventDetails.builder().foodProvided(true).build());
		when(registrationRepository.save(reg)).thenReturn(reg);

		EventRegistrationDto dto = EventRegistrationDto.builder()
				.gdprConsent(true).photoConsent(true)
				.transportationNeeded(false).build();

		eventService.updateRegistration(eventId, "test@msg.group", dto);

		assertThat(reg.getTransportationDetails()).isNull();
	}

	// ---------- getEventStatistics ----------

	@Test
	void getEventStatistics_returnsAggregatedCounts() {
		UUID eventId = UUID.randomUUID();
		Event event = Event.builder().id(eventId).location(EventLocation.CLUJ).build();
		User u1 = User.builder().id(UUID.randomUUID()).email("a.a@msg.group")
				.userDetails(UserDetails.builder().firstName("A").lastName("A").build()).build();
		User u2 = User.builder().id(UUID.randomUUID()).email("b.b@msg.group").build();
		Registration r1 = Registration.builder().user(u1).event(event).photoConsent(true)
				.accommodationNeeded(true).transportationNeeded(false)
				.foodPreference(com.cluj1.eventapp.model.enums.FoodPreference.VEGAN)
				.registrationDate(OffsetDateTime.now())
				.attendanceRecord(AttendanceRecord.builder().checkInTime(OffsetDateTime.now()).build())
				.build();
		Registration r2 = Registration.builder().user(u2).event(event).photoConsent(false)
				.accommodationNeeded(false).transportationNeeded(true)
				.registrationDate(OffsetDateTime.now())
				.build();

		when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
		when(recipientPoolService.getRecipientsCountForEvent(EventLocation.CLUJ)).thenReturn(10);
		when(registrationRepository.findByEventId(eventId)).thenReturn(List.of(r1, r2));

		EventStatisticsDto stats = eventService.getEventStatistics(eventId);

		assertThat(stats.getInvitedCount()).isEqualTo(10);
		assertThat(stats.getRegistrationCount()).isEqualTo(2);
		assertThat(stats.getParticipantCount()).isEqualTo(1);
		assertThat(stats.getAccommodationPercentage()).isEqualTo(50.0);
		assertThat(stats.getTransportPercentage()).isEqualTo(50.0);
		assertThat(stats.getPhotoConsentPercentage()).isEqualTo(50.0);
		assertThat(stats.getFoodPreferencePercentages()).containsKey("VEGAN").containsKey("NONE");
		assertThat(stats.getParticipants()).hasSize(2);
		assertThat(stats.getParticipants().get(0).getName()).isEqualTo("A A");
		assertThat(stats.getParticipants().get(0).getStatus()).isEqualTo("CHECKED_IN");
		assertThat(stats.getParticipants().get(1).getName()).isEqualTo("Unknown");
		assertThat(stats.getParticipants().get(1).getStatus()).isEqualTo("REGISTERED");
	}

	@Test
	void getEventStatistics_returnsEmptyPercentages_whenNoRegistrations() {
		UUID eventId = UUID.randomUUID();
		Event event = Event.builder().id(eventId).location(EventLocation.CLUJ).build();
		when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
		when(recipientPoolService.getRecipientsCountForEvent(EventLocation.CLUJ)).thenReturn(0);
		when(registrationRepository.findByEventId(eventId)).thenReturn(List.of());

		EventStatisticsDto stats = eventService.getEventStatistics(eventId);

		assertThat(stats.getRegistrationCount()).isZero();
		assertThat(stats.getAccommodationPercentage()).isZero();
		assertThat(stats.getFoodPreferencePercentages()).isEmpty();
	}

	@Test
	void getEventStatistics_swallowsRecipientPoolException_andReportsZero() {
		UUID eventId = UUID.randomUUID();
		Event event = Event.builder().id(eventId).location(EventLocation.CLUJ).build();
		when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
		when(recipientPoolService.getRecipientsCountForEvent(EventLocation.CLUJ))
				.thenThrow(new RuntimeException("boom"));
		when(registrationRepository.findByEventId(eventId)).thenReturn(List.of());

		EventStatisticsDto stats = eventService.getEventStatistics(eventId);

		assertThat(stats.getInvitedCount()).isZero();
	}

	@Test
	void getEventStatistics_throws_whenEventNotFound() {
		UUID eventId = UUID.randomUUID();
		when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.getEventStatistics(eventId))
				.isInstanceOf(IllegalArgumentException.class);
	}



	@Test
	void generateCheckInCodes_returnsExistingCodes_whenAlreadyGenerated() {
		UUID id = UUID.randomUUID();
		Event event = Event.builder().id(id).status(EventStatus.PUBLISHED).build();
		com.cluj1.eventapp.model.EventDetails details = com.cluj1.eventapp.model.EventDetails.builder()
				.eventCode("EXIST1").qrCodeContent("data:image/png;base64,X").build();
		when(eventRepository.findById(id)).thenReturn(Optional.of(event));
		when(eventDetailsRepository.findByEvent(event)).thenReturn(details);

		com.cluj1.eventapp.dto.CheckInCodesDto codes = eventService.generateCheckInCodes(id);

		assertThat(codes.getEventCode()).isEqualTo("EXIST1");
		assertThat(codes.getQrCodeContent()).isEqualTo("data:image/png;base64,X");
		verify(eventDetailsRepository, never()).save(any());
	}

	@Test
	void generateCheckInCodes_generatesAndSaves_whenNoCodesYet() {
		UUID id = UUID.randomUUID();
		Event event = Event.builder().id(id).status(EventStatus.PUBLISHED).build();
		com.cluj1.eventapp.model.EventDetails details = com.cluj1.eventapp.model.EventDetails.builder().build();
		when(eventRepository.findById(id)).thenReturn(Optional.of(event));
		when(eventDetailsRepository.findByEvent(event)).thenReturn(details);
		when(eventDetailsRepository.existsByEventCode(any())).thenReturn(false);

		com.cluj1.eventapp.dto.CheckInCodesDto codes = eventService.generateCheckInCodes(id);

		assertThat(codes.getEventCode()).hasSize(6);
		assertThat(codes.getQrCodeContent()).startsWith("data:image/png;base64,");
		verify(eventDetailsRepository).save(details);
	}

	@Test
	void generateCheckInCodes_throws_whenEventNotPublished() {
		UUID id = UUID.randomUUID();
		Event event = Event.builder().id(id).status(EventStatus.DRAFT).build();
		com.cluj1.eventapp.model.EventDetails details = com.cluj1.eventapp.model.EventDetails.builder().build();
		when(eventRepository.findById(id)).thenReturn(Optional.of(event));
		when(eventDetailsRepository.findByEvent(event)).thenReturn(details);

		assertThatThrownBy(() -> eventService.generateCheckInCodes(id))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void generateCheckInCodes_throws_whenEventNotFound() {
		UUID id = UUID.randomUUID();
		when(eventRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.generateCheckInCodes(id))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
