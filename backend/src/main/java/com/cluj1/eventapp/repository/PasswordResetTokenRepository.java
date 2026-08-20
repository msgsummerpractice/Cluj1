package com.cluj1.eventapp.repository;

import com.cluj1.eventapp.model.PasswordResetToken;
import com.cluj1.eventapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findByUserId(UUID userId);

    void deleteByUser(User user);
}
