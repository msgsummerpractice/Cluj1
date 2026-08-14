package com.cluj1.eventapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cluj1.eventapp.model.EventDetails;
import com.cluj1.eventapp.repository.EventDetailsRepository;

@ExtendWith(MockitoExtension.class)
class EventDetailsServiceTest {

    @Mock
    private EventDetailsRepository eventDetailsRepository;

    @InjectMocks
    private EventDetailsService eventDetailsService;

    @Test
    void getEventDetailsByIdReturnEventDetailsWhenIdExists() {
        UUID id = UUID.randomUUID();
        EventDetails expected = EventDetails.builder()
                .id(id)
                .description("Detailed event description")
                .foodProvided(true)
                .eventCode("AB12CD")
                .build();

        when(eventDetailsRepository.findById(id)).thenReturn(Optional.of(expected));

        EventDetails result = eventDetailsService.getEventDetailsById(id);

        assertThat(result).isEqualTo(expected);
        verify(eventDetailsRepository).findById(id);
    }

    @Test
    void getEventDetailsByIdThrowRuntimeExceptionWhenIdDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(eventDetailsRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventDetailsService.getEventDetailsById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Event details not found for id: " + id);

        verify(eventDetailsRepository).findById(id);
    }
}