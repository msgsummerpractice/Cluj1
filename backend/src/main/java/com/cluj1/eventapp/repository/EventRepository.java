package com.cluj1.eventapp.repository;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.enums.EventLocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.user.id = :user_id AND r.event.eventStartDate > :now")
    int countUpcomingEventsForUsers(@Param("now") OffsetDateTime now, @Param("user_id") UUID user_id);

    @Query("SELECT e FROM Event e WHERE e.status = com.cluj1.eventapp.model.enums.EventStatus.PUBLISHED " +
            "AND e.registrationEndDate >= :now " +
            "AND (e.location = com.cluj1.eventapp.model.enums.EventLocation.ALL OR e.location = :userLocation)")
    List<Event> findEligibleEvents(@Param("now") OffsetDateTime now, @Param("userLocation") EventLocation userLocation);

    @Query("SELECT e FROM Event e WHERE e.status = com.cluj1.eventapp.model.enums.EventStatus.PUBLISHED " +
            "AND e.registrationEndDate >= :now " +
            "AND e.location = com.cluj1.eventapp.model.enums.EventLocation.ALL")
    List<Event> findAllLocationEligibleEvents(@Param("now") OffsetDateTime now);

    @Query("SELECT e FROM Event e WHERE e.id = CAST(:code AS uuid) OR e.name = :code")
    Optional<Event> findByCodeOrId(@Param("code") String code);
}
