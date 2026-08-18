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

    /**
     * Returns published events whose {@code registrationEndDate} is on or after
     * {@code now}
     * and whose location is either {@code ALL} or matches {@code userLocation}.
     *
     * @param now          the reference instant used to filter out expired
     *                     registrations
     * @param userLocation the participant's specific location (must not be
     *                     {@code null})
     * @return matching events
     */
    @Query("SELECT e FROM Event e WHERE e.status = com.cluj1.eventapp.model.enums.EventStatus.PUBLISHED " +
            "AND e.registrationEndDate >= :now " +
            "AND (e.location = com.cluj1.eventapp.model.enums.EventLocation.ALL OR e.location = :userLocation)")
    List<Event> findEligibleEvents(@Param("now") OffsetDateTime now, @Param("userLocation") EventLocation userLocation);

    /**
     * Returns published events whose {@code registrationEndDate} is on or after
     * {@code now}
     * and whose location is {@code ALL}. Used for participants with no specific
     * location
     * (e.g. {@code REMOTE} users or users without profile details).
     *
     * @param now the reference instant used to filter out expired registrations
     * @return matching events
     */
    @Query("SELECT e FROM Event e WHERE e.status = com.cluj1.eventapp.model.enums.EventStatus.PUBLISHED " +
            "AND e.registrationEndDate >= :now " +
            "AND e.location = com.cluj1.eventapp.model.enums.EventLocation.ALL")
    List<Event> findAllLocationEligibleEvents(@Param("now") OffsetDateTime now);

    @Query("SELECT e FROM Event e WHERE e.id = CAST(:code AS uuid) OR e.name = :code")
    Optional<Event> findByCodeOrId(@Param("code") String code);
}
