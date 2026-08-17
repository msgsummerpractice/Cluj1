package com.cluj1.eventapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

	@Mock
	private EventRepository eventRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private EventMapper eventMapper;

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
		Event firstEvent = Event.builder()
				.id(UUID.randomUUID())
				.name("Opening Ceremony")
				.build();
		Event secondEvent = Event.builder()
				.id(UUID.randomUUID())
				.name("Closing Ceremony")
				.build();

		EventDto firstDto = EventDto.builder()
				.id(firstEvent.getId())
				.name(firstEvent.getName())
				.build();
		EventDto secondDto = EventDto.builder()
				.id(secondEvent.getId())
				.name(secondEvent.getName())
				.build();

		when(eventRepository.findAll()).thenReturn(List.of(firstEvent, secondEvent));
		when(eventMapper.toDto(firstEvent)).thenReturn(firstDto);
		when(eventMapper.toDto(secondEvent)).thenReturn(secondDto);

		List<EventDto> result = eventService.getAllEvents();

		assertThat(result).containsExactly(firstDto, secondDto);
		verify(eventRepository).findAll();
		verify(eventMapper).toDto(firstEvent);
		verify(eventMapper).toDto(secondEvent);
	}

	@Test
	void getAllEventsWhenRepositoryIsEmptyReturnsEmptyList() {
		when(eventRepository.findAll()).thenReturn(List.of());

		List<EventDto> result = eventService.getAllEvents();

		assertThat(result).isEmpty();
		verify(eventRepository).findAll();
		verify(eventMapper, never()).toDto(org.mockito.ArgumentMatchers.any(Event.class));
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

		verify(eventRepository).save(argThat(e -> e.getEventDetails().getFoodProvided() == null));
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
}
