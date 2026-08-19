package com.cluj1.eventapp.repository;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import com.cluj1.eventapp.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.user.id = :user_id AND r.event.eventStartDate > :now")
    int countUpcomingEventsForUsers(@Param("now") OffsetDateTime now, @Param("user_id") UUID user_id);

    @Query("SELECT COUNT(r) > 0 FROM Registration r WHERE r.event.id = :eventId AND r.user.id = :userId")
    boolean existsByEventIdAndUserId(@Param("eventId") UUID eventId, @Param("userId") UUID userId);

    @Query("SELECT e FROM Event e WHERE e.id = CAST(:code AS uuid) OR e.name = :code")
    Optional<Event> findByCodeOrId(@Param("code") String code);

}