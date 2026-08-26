package com.cluj1.eventapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.EventDetails;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;

@ExtendWith(MockitoExtension.class)
class EventPublishMailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RecipientPoolService recipientPoolService;

    @InjectMocks
    private EventPublishMailService mailService;

    private Event event;
    private EventDetails details;


    private static final byte[] PNG_BYTES = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 1, 0, 0, 0, 1,
            8, 6, 0, 0, 0, 31, 21, (byte) 196, (byte) 137
    };

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "sender", "noreply@msg.group");
        ReflectionTestUtils.setField(mailService, "eventUrl", "http://localhost:4200/events");
        ReflectionTestUtils.setField(mailService, "recipientPoolService", recipientPoolService);

        event = Event.builder()
                .id(UUID.randomUUID())
                .name("Tech Meetup")
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.PUBLISHED)
                .eventStartDate(OffsetDateTime.of(2026, 3, 10, 18, 0, 0, 0, ZoneOffset.UTC))
                .build();

        details = EventDetails.builder()
                .description("Description")
                .foodProvided(true)
                .poster(PNG_BYTES)
                .build();
        event.setEventDetails(details);
    }

    private MimeMessage newMimeMessage() {
        return new MimeMessage((Session) null);
    }

    @Test
    void sendEventPublishedEmail_sendsMessage_withValidPngPoster() throws Exception {
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        mailService.sendEventPublishedEmail("recipient@msg.group", event);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject())
                .isEqualTo("New event published | Eveniment nou publicat: Tech Meetup");
        assertThat(captor.getValue().getAllRecipients()[0].toString()).isEqualTo("recipient@msg.group");
    }

    @Test
    void sendEventPublishedEmail_sendsMessage_whenPosterIsUnsupportedType() throws Exception {
        details.setPoster("not a real image".getBytes());
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        mailService.sendEventPublishedEmail("recipient@msg.group", event);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendEventPublishedEmail_sendsMessage_whenPosterIsNull() throws Exception {
        details.setPoster(null);
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        mailService.sendEventPublishedEmail("recipient@msg.group", event);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendEventPublishedEmail_sendsMessage_whenPosterIsEmptyByteArray() throws Exception {
        details.setPoster(new byte[0]);
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        mailService.sendEventPublishedEmail("recipient@msg.group", event);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendEventPublishedEmail_sendsMessage_whenStartDateIsNull() throws Exception {
        event.setEventStartDate(null);
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        mailService.sendEventPublishedEmail("recipient@msg.group", event);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendEventPublishedEmail_throwsIllegalArgumentException_whenEventDetailsMissing() {
        event.setEventDetails(null);

        assertThatThrownBy(() -> mailService.sendEventPublishedEmail("recipient@msg.group", event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(event.getId().toString());

        verifyNoInteractions(mailSender);
    }

    @Test
    void sendEventPublishedEmail_swallowsMailSenderException() {
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);
        org.mockito.Mockito.doThrow(new MailSendException("SMTP down"))
                .when(mailSender).send(any(MimeMessage.class));


        mailService.sendEventPublishedEmail("recipient@msg.group", event);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void notifyRecipients_sendsOneEmailPerResolvedRecipient() {
        User user1 = User.builder().email("u1@msg.group").build();
        User user2 = User.builder().email("u2@msg.group").build();
        when(recipientPoolService.resolveRecipients(event.getLocation()))
                .thenReturn(List.of(user1, user2));
        when(mailSender.createMimeMessage()).thenReturn(newMimeMessage(), newMimeMessage());

        mailService.notifyRecipients(event);

        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void notifyRecipients_sendsNoEmail_whenRecipientListIsEmpty() {
        when(recipientPoolService.resolveRecipients(event.getLocation())).thenReturn(List.of());

        mailService.notifyRecipients(event);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendHtmlMessage_sendsMessage_whenNoPosterProvided() throws Exception {
        MimeMessage realMessage = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(realMessage);

        mailService.sendHtmlMessage("to@msg.group", "Subject", "<p>Body</p>", null, null);

        verify(mailSender).send(realMessage);
    }

    @Test
    void sendHtmlMessage_sendsMessage_withInlinePoster_whenPosterAndMimeTypeProvided() throws Exception {
        MimeMessage realMessage = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(realMessage);

        mailService.sendHtmlMessage("to@msg.group", "Subject", "<p>Body</p>", PNG_BYTES, "image/png");

        verify(mailSender).send(realMessage);
    }
}




