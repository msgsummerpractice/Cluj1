package com.cluj1.eventapp.service;

import java.time.format.DateTimeFormatter;
import java.util.List;

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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm");
    private static final String POSTER_CONTENT_ID = "eventPoster";

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

            String eventLink = eventUrl + "/" + event.getId();
            String startDate = event.getEventStartDate() != null
                    ? event.getEventStartDate().format(DATE_FORMATTER)
                    : "To be announced";

            StringBuilder htmlBody = new StringBuilder();
            htmlBody.append("<div style=\"font-family: Arial, sans-serif; padding: 20px;\">")
                    .append("<h2>New event published</h2>");

            if (poster != null && poster.length > 0) {
                htmlBody.append("<img src=\"cid:").append(POSTER_CONTENT_ID)
                        .append("\" alt=\"Event poster\" style=\"max-width: 100%; border-radius: 5px;\" />");
            }

            htmlBody.append("<p><strong>Event:</strong> ").append(escapeHtml(event.getName())).append("<br/>")
                    .append("<strong>Date:</strong> ").append(startDate).append("<br/>")
                    .append("<strong>Location:</strong> ").append(event.getLocation()).append("</p>")
                    .append("<p>Click the link below to see all the details and register:</p>")
                    .append("<a href=\"").append(eventLink)
                    .append("\" style=\"background-color: #8b143d; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;\">View Event</a>")
                    .append("</div>");

            sendHtmlMessage(recipientEmail, "New event published: " + event.getName(), htmlBody.toString(), poster);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendHtmlMessage(String to, String subject, String htmlBody, byte[] poster) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(sender);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        if (poster != null && poster.length > 0) {
            helper.addInline(POSTER_CONTENT_ID, new ByteArrayResource(poster), "image/png");
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
