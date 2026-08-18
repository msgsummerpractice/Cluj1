package com.cluj1.eventapp.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.EventDetails;
import com.cluj1.eventapp.model.User;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EventPublishMailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final String POSTER_CONTENT_ID = "eventPoster";
    private static final Set<String> SUPPORTED_POSTER_TYPES = Set.of("image/png", "image/jpeg");
    private static final Tika TIKA = new Tika();

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private RecipientPoolService recipientPoolService;

    @Value("${spring.mail.username}")
    private String sender;

    @Value("${app.event-url:http://localhost:4200/events}")
    private String eventUrl;

    public void notifyRecipients(Event event) {
        List<User> recipients = recipientPoolService.resolveRecipients(event.getLocation());
        for (User recipient : recipients) {
            sendEventPublishedEmail(recipient.getEmail(), event);
        }
    }

    public void sendEventPublishedEmail(String recipientEmail, Event event) {
        try {
            EventDetails details = event.getEventDetails();
            byte[] poster = details != null ? details.getPoster() : null;
            String posterMimeType = detectPosterMimeType(poster);
            if (posterMimeType == null) {
                poster = null;
            }

            String eventLink = eventUrl + "/" + event.getId();
            String startDate = event.getEventStartDate() != null
                    ? event.getEventStartDate().format(DATE_FORMATTER)
                    : "To be announced";

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

            sendHtmlMessage(recipientEmail, "New event published | Eveniment nou publicat: " + event.getName(),
                    htmlBody.toString(), poster,
                    posterMimeType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String detectPosterMimeType(byte[] poster) {
        if (poster == null || poster.length == 0) {
            return null;
        }
        String detected = TIKA.detect(poster);
        return SUPPORTED_POSTER_TYPES.contains(detected) ? detected : null;
    }

    public void sendHtmlMessage(String to, String subject, String htmlBody, byte[] poster, String posterMimeType)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(sender);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        if (poster != null && poster.length > 0 && posterMimeType != null) {
            helper.addInline(POSTER_CONTENT_ID, new ByteArrayResource(poster), posterMimeType);
        }

        mailSender.send(message);
    }

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
