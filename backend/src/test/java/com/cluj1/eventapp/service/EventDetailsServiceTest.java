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

import com.cluj1.eventapp.dto.EventDetailsDto;
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

    @Test
    void getEventDetailsByEventId_returnsDtoWithAllFields_whenDetailsExist() {
        UUID eventId = UUID.randomUUID();
        UUID detailsId = UUID.randomUUID();
        EventDetails details = EventDetails.builder()
                .id(detailsId)
                .description("desc")
                .foodProvided(true)
                .eventCode("ABC123")
                .qrCodeContent("data:image/png;base64,AAA")
                .poster(new byte[] { 1, 2, 3 })
                .build();
        when(eventDetailsRepository.findByEventId(eventId)).thenReturn(Optional.of(details));

        EventDetailsDto dto = eventDetailsService.getEventDetailsByEventId(eventId);

        assertThat(dto.getId()).isEqualTo(detailsId);
        assertThat(dto.getEventId()).isEqualTo(eventId);
        assertThat(dto.getDescription()).isEqualTo("desc");
        assertThat(dto.getFoodProvided()).isTrue();
        assertThat(dto.getEventCode()).isEqualTo("ABC123");
        assertThat(dto.getQrCodeContent()).isEqualTo("data:image/png;base64,AAA");
        assertThat(dto.getHasPoster()).isTrue();
    }

    @Test
    void getEventDetailsByEventId_setsHasPosterFalse_whenPosterIsNull() {
        UUID eventId = UUID.randomUUID();
        EventDetails details = EventDetails.builder()
                .id(UUID.randomUUID())
                .foodProvided(false)
                .poster(null)
                .build();
        when(eventDetailsRepository.findByEventId(eventId)).thenReturn(Optional.of(details));

        EventDetailsDto dto = eventDetailsService.getEventDetailsByEventId(eventId);

        assertThat(dto.getHasPoster()).isFalse();
    }

    @Test
    void getEventDetailsByEventId_setsHasPosterFalse_whenPosterIsEmpty() {
        UUID eventId = UUID.randomUUID();
        EventDetails details = EventDetails.builder()
                .id(UUID.randomUUID())
                .foodProvided(true)
                .poster(new byte[0])
                .build();
        when(eventDetailsRepository.findByEventId(eventId)).thenReturn(Optional.of(details));

        EventDetailsDto dto = eventDetailsService.getEventDetailsByEventId(eventId);

        assertThat(dto.getHasPoster()).isFalse();
    }

    @Test
    void getEventDetailsByEventId_throwsRuntimeException_whenDetailsMissing() {
        UUID eventId = UUID.randomUUID();
        when(eventDetailsRepository.findByEventId(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventDetailsService.getEventDetailsByEventId(eventId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Event details not found for event id: " + eventId);
    }

    @Test
    void getPosterByEventId_returnsPoster_whenPresentAndNonEmpty() {
        UUID eventId = UUID.randomUUID();
        byte[] poster = { 1, 2, 3 };
        when(eventDetailsRepository.findPosterByEventId(eventId)).thenReturn(Optional.of(poster));

        Optional<byte[]> result = eventDetailsService.getPosterByEventId(eventId);

        assertThat(result).contains(poster);
    }

    @Test
    void getPosterByEventId_returnsEmpty_whenPosterBytesAreEmpty() {
        UUID eventId = UUID.randomUUID();
        when(eventDetailsRepository.findPosterByEventId(eventId)).thenReturn(Optional.of(new byte[0]));

        Optional<byte[]> result = eventDetailsService.getPosterByEventId(eventId);

        assertThat(result).isEmpty();
    }

    @Test
    void getPosterByEventId_returnsEmpty_whenPosterNotFound() {
        UUID eventId = UUID.randomUUID();
        when(eventDetailsRepository.findPosterByEventId(eventId)).thenReturn(Optional.empty());

        Optional<byte[]> result = eventDetailsService.getPosterByEventId(eventId);

        assertThat(result).isEmpty();
    }
}