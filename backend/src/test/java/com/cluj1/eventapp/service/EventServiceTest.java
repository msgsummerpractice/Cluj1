package com.cluj1.eventapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cluj1.eventapp.dto.EventDto;
import com.cluj1.eventapp.mapper.EventMapper;
import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.repository.EventRepository;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

	@Mock
	private EventRepository eventRepository;

	@Mock
	private EventMapper eventMapper;

	@InjectMocks
	private EventService eventService;

	@Test
	void getAllEvents_returnsMappedEventsInRepositoryOrder() {
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
	void getAllEvents_whenRepositoryIsEmpty_returnsEmptyList() {
		when(eventRepository.findAll()).thenReturn(List.of());

		List<EventDto> result = eventService.getAllEvents();

		assertThat(result).isEmpty();
		verify(eventRepository).findAll();
		verify(eventMapper, never()).toDto(org.mockito.ArgumentMatchers.any(Event.class));
	}

}
