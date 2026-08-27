package com.cluj1.eventapp.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.EventDetails;
import com.cluj1.eventapp.model.User;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

/**
 * Sends bilingual HTML notifications when an event is published.
 *
 * <p>
 * Event posters are embedded inline in the message when their detected MIME
 * type is PNG or JPEG. Other poster formats are ignored.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class EventPublishMailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final String POSTER_CONTENT_ID = "eventPoster";
    private static final Set<String> SUPPORTED_POSTER_TYPES = Set.of("image/png", "image/jpeg");
    private static final Tika TIKA = new Tika();

    private final JavaMailSender mailSender;

    @Autowired
    private RecipientPoolService recipientPoolService;

    @Value("${spring.mail.username}")
    private String sender;

    @Value("${app.event-url:http://localhost:4200/events}")
    private String eventUrl;

    /**
     * Resolves the recipients for an event location and notifies them all in a
     * single email, so no recipient can see who else received it.
     *
     * @param event the published event to include in the notification
     */
    public void notifyRecipients(Event event) {
        List<User> recipients = recipientPoolService.resolveRecipients(event.getLocation());
        if (recipients.isEmpty()) {
            return;
        }
        List<String> recipientEmails = recipients.stream().map(User::getEmail).toList();
        sendEventPublishedEmail(recipientEmails, event);
    }

    /**
     * Builds and sends a single bilingual event-published email to all given
     * recipients, placed in BCC so they remain hidden from one another.
     *
     * <p>
     * Missing event details cause an {@link IllegalArgumentException}. Mail
     * delivery failures are logged and do not propagate to the caller.
     * </p>
     *
     * @param recipientEmails the email addresses that should receive the message
     * @param event           the published event to include in the message
     * @throws IllegalArgumentException if the event has no details
     */
    public void sendEventPublishedEmail(List<String> recipientEmails, Event event) {
        EventDetails details = event.getEventDetails();
        if (details == null) {
            throw new IllegalArgumentException("Event details are missing for event: " + event.getId());
        }

        byte[] poster = details.getPoster();
        String posterMimeType = detectPosterMimeType(poster);
        if (posterMimeType == null) {
            poster = null;
        }

        String eventLink = eventUrl + "/" + event.getId();
        String startDate = event.getEventStartDate() != null
                ? event.getEventStartDate().format(DATE_FORMATTER)
                : "To be announced";

        String htmlBody = buildEmail(event, eventLink, startDate, poster);

        try {
            sendHtmlMessage(recipientEmails, "New event published | Eveniment nou publicat: " + event.getName(),
                    htmlBody, poster,
                    posterMimeType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the HTML body containing English and Romanian event information.
     *
     * @param event     the event whose information is displayed
     * @param eventLink the URL used by both registration links
     * @param startDate the already formatted event date
     * @param poster    the validated poster bytes, or {@code null} when no poster
     *                  is available
     * @return the HTML email body
     */
    private String buildEmail(Event event, String eventLink, String startDate, byte[] poster) {
        StringBuilder htmlBody = new StringBuilder();
        htmlBody.append("<div style=\"font-family: Arial, sans-serif; padding: 20px;\">")
                .append("<h2>New event published</h2>");

        if (poster != null && poster.length > 0) {
            htmlBody.append("<img src=\"cid:").append(POSTER_CONTENT_ID)
                    .append("\" alt=\"Event poster\" style=\"max-width: 250px; width: 100%; height: auto; border-radius: 5px; display: block; margin: 15px 0;\" />");
        }

        htmlBody.append("<p><strong>Event:</strong> ").append(escapeHtml(event.getName())).append("<br/>")
                .append("<strong>Date:</strong> ").append(startDate).append("<br/>")
                .append("<strong>Location:</strong> ").append(event.getLocation()).append("</p>")
                .append("<p>Click the button below to see all the details and register:</p>")
                .append("<a href=\"").append(eventLink)
                .append("\" style=\"background-color: #8b143d; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;\">View Event</a>")
                .append("<p style=\"color: #666; font-size: 0.9em; margin-top: 15px;\"><em>(Versiunea în limba română se află mai jos / Find the Romanian text below)</em></p>")
                .append("<hr style=\"margin: 25px 0; border: none; border-top: 1px solid #ddd;\"/>");

        htmlBody.append("<h2>Eveniment nou publicat</h2>")
                .append("<p><strong>Eveniment:</strong> ").append(escapeHtml(event.getName())).append("<br/>")
                .append("<strong>Data:</strong> ").append(startDate).append("<br/>")
                .append("<strong>Locație:</strong> ").append(event.getLocation()).append("</p>")
                .append("<p>Apasă butonul de mai jos pentru a vedea toate detaliile și a te înregistra:</p>")
                .append("<a href=\"").append(eventLink)
                .append("\" style=\"background-color: #8b143d; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;\">Vizualizează Evenimentul</a>")
                .append("</div>");

        return htmlBody.toString();
    }

    /**
     * Detects whether poster bytes use a MIME type supported for inline display.
     *
     * @param poster the poster bytes to inspect
     * @return {@code image/png} or {@code image/jpeg} when supported; otherwise
     *         {@code null}
     */
    private String detectPosterMimeType(byte[] poster) {
        if (poster == null || poster.length == 0) {
            return null;
        }
        String detected = TIKA.detect(poster);
        return SUPPORTED_POSTER_TYPES.contains(detected) ? detected : null;
    }

    /**
     * Sends a single HTML email to all recipients via BCC, so recipients cannot
     * see one another, and optionally attaches a poster as an inline resource.
     *
     * @param recipientEmails the BCC recipient email addresses
     * @param subject         the email subject
     * @param htmlBody        the HTML message body
     * @param poster          poster bytes to embed, or {@code null} when no poster
     *                        should be embedded
     * @param posterMimeType  the poster MIME type, or {@code null} when no poster
     *                        is
     *                        supplied
     * @throws MessagingException if the mail message cannot be created or sent
     */
    public void sendHtmlMessage(List<String> recipientEmails, String subject, String htmlBody, byte[] poster,
            String posterMimeType) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(sender);
        helper.setTo(sender);
        helper.setBcc(recipientEmails.toArray(new String[0]));
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        if (poster != null && poster.length > 0 && posterMimeType != null) {
            helper.addInline(POSTER_CONTENT_ID, new ByteArrayResource(poster), posterMimeType);
        }

        mailSender.send(message);
    }

    /**
     * Escapes user-provided text before inserting it into HTML markup.
     *
     * @param value the text to escape
     * @return escaped text, or an empty string for {@code null}
     */
    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
