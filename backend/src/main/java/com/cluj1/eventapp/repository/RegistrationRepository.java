package com.cluj1.eventapp.repository;

import com.cluj1.eventapp.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.user.id = :userId")
    int countTotalRegistrationsPerUser(@Param("userId") UUID userId);

    Optional<Registration> findByEventIdAndUserId(UUID eventId, UUID userId);

    Optional<Registration> findByUserIdAndEventId(UUID userId, UUID eventId);

    @Query("SELECT r FROM Registration r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH u.userDetails ud " +
            "LEFT JOIN FETCH r.transportationDetails td " +
            "JOIN FETCH r.event e " +
            "WHERE r.event.id = :eventId " +
            "ORDER BY ud.lastName ASC, ud.firstName ASC")
    List<Registration> findAllByEventIdWithDetails(@Param("eventId") UUID eventId);
}
