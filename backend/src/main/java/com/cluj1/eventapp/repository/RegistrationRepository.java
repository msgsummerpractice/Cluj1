package com.cluj1.eventapp.repository;

import com.cluj1.eventapp.model.Registration;
import org.hibernate.boot.models.JpaAnnotations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.user.id = :userId")
    int countTotalRegistrationsPerUser(@Param("userId") UUID userId);

    Optional<Registration> findByUserIdAndEventId(UUID userId, UUID eventId);
}
