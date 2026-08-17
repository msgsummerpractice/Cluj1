package com.cluj1.eventapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.cluj1.eventapp.model.PasswordResetToken;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.repository.PasswordResetTokenRepository;
import com.cluj1.eventapp.repository.UserRepository;

import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("old-password-hash")
                .role(Role.PARTICIPANT)
                .isActive(true)
                .build();

        ReflectionTestUtils.setField(passwordResetService, "sender", "noreply@example.com");
        ReflectionTestUtils.setField(passwordResetService, "passwordResetUrl", "http://localhost:4200/reset-password");
    }

    @Test
    void createPasswordResetToken_existingUser_replacesOldTokenAndSendsMail() {
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        passwordResetService.createPasswordResetToken("user@example.com");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        verify(tokenRepository).deleteByUser(user);
        verify(tokenRepository).flush();
        verify(tokenRepository).save(tokenCaptor.capture());
        verify(mailSender).send(mimeMessage);

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getTokenHash()).isNotBlank();
        assertThat(savedToken.getExpiresAt()).isAfter(OffsetDateTime.now().plusMinutes(55));
    }

    @Test
    void createPasswordResetToken_missingUser_doesNothing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        passwordResetService.createPasswordResetToken("missing@example.com");

        verify(tokenRepository, never()).deleteByUser(any());
        verify(tokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void resetPassword_validToken_updatesPasswordAndMarksTokenUsed() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash("token-123")
                .expiresAt(OffsetDateTime.now().plusMinutes(30))
                .build();

        when(tokenRepository.findByTokenHash("token-123")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("NewPassword1!")).thenReturn("encoded-password");

        passwordResetService.resetPassword("token-123", "NewPassword1!", "NewPassword1!");

        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(resetToken.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(tokenRepository).save(resetToken);
    }

    @Test
    void resetPassword_passwordMismatch_throwsException() {
        assertThatThrownBy(() -> passwordResetService.resetPassword("token-123", "NewPassword1!", "Different1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passwords do not match");

        verify(tokenRepository, never()).findByTokenHash(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_usedToken_throwsException() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash("used-token")
                .expiresAt(OffsetDateTime.now().plusMinutes(30))
                .usedAt(OffsetDateTime.now().minusMinutes(1))
                .build();

        when(tokenRepository.findByTokenHash("used-token")).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword("used-token", "NewPassword1!", "NewPassword1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This reset link has already been used");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_expiredToken_throwsException() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash("expired-token")
                .expiresAt(OffsetDateTime.now().minusMinutes(1))
                .build();

        when(tokenRepository.findByTokenHash("expired-token")).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "NewPassword1!", "NewPassword1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This reset link has expired");

        verify(userRepository, never()).save(any());
    }
}