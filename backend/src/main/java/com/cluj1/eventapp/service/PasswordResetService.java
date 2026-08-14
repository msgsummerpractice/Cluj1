package com.cluj1.eventapp.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.cluj1.eventapp.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cluj1.eventapp.model.PasswordResetToken;
import com.cluj1.eventapp.repository.PasswordResetTokenRepository;
import com.cluj1.eventapp.repository.UserRepository;

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

    public void createPasswordResetToken(String email){
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null){
            return;
        }

        tokenRepo.deleteByUser(user);
        tokenRepo.flush();

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build();
        tokenRepo.save(resetToken);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Password Reset Request");
        message.setText("To reset your password, use the following token: " + rawToken);
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

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        resetToken.setUsedAt(OffsetDateTime.now());
        tokenRepo.save(resetToken);
    }

}