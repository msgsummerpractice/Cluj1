package com.cluj1.eventapp.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.cluj1.eventapp.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cluj1.eventapp.model.PasswordResetToken;
import com.cluj1.eventapp.repository.PasswordResetTokenRepository;
import com.cluj1.eventapp.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;

@Service
public class PasswordResetService {
    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordResetTokenRepository tokenRepo;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${spring.mail.username}")
    private String sender;

    @Value("${app.password-reset-url:http://localhost:4200/reset-password}")
    private String passwordResetUrl;

    @Transactional
    public void createPasswordResetToken(String email){
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null){
            return;
        }

        tokenRepo.deleteByUser(user);
        tokenRepo.flush();

        String rawToken = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(rawToken)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build();
        tokenRepo.save(resetToken);

        try {
            String resetLink = passwordResetUrl + "?token=" + rawToken;
            String htmlBody = "<div style=\"font-family: Arial, sans-serif; padding: 20px;\">" +
                    "<h2>Password Reset Request</h2>" +
                    "<p>To reset your password, click the link below:</p>" +
                    "<a href=\"" + resetLink + "\" style=\"background-color: #8b143d; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;\">Reset Password</a>" +
                    "<p>If you didn't request this, please ignore this email.</p>" +
                    "</div>";
            
            sendHtmlMessage(user.getEmail(), "Password Reset Request", htmlBody);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendHtmlMessage(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(sender);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        mailSender.send(message);
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword){
        if(!newPassword.equals(confirmPassword)){
            throw new IllegalArgumentException("Passwords do not match");
        }
        PasswordResetToken resetToken = tokenRepo.findByTokenHash(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (resetToken.getUsedAt() != null){
            throw new IllegalArgumentException("This reset link has already been used");
        }
        if(resetToken.getExpiresAt().isBefore(OffsetDateTime.now())){
            throw new IllegalArgumentException("This reset link has expired");
        }

        User tokenUser = resetToken.getUser();
        
        User user = userRepo.findById(tokenUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String encodedPassword = passwordEncoder.encode(newPassword);
        entityManager.createQuery("UPDATE User u SET u.passwordHash = :passwordHash WHERE u.id = :userId")
            .setParameter("passwordHash", encodedPassword)
            .setParameter("userId", user.getId())
            .executeUpdate();
        entityManager.flush();
        user.setPasswordHash(encodedPassword);

        resetToken.setUsedAt(OffsetDateTime.now());
        tokenRepo.save(resetToken);
        tokenRepo.flush();
    }
}