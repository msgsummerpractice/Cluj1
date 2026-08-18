package com.cluj1.eventapp.repository;

import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.EventDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventDetailsRepository extends JpaRepository<EventDetails, UUID> {

    Optional<EventDetails> findByEventId(UUID eventId);

    @Query("select d.poster from EventDetails d where d.event.id = :eventId")
    Optional<byte[]> findPosterByEventId(@Param("eventId") UUID eventId);

    EventDetails findByEvent(Event event);

    boolean existsByEventCode(String code);

    @Query("SELECT ed.event FROM EventDetails ed WHERE ed.eventCode = :eventCode")
    Optional<Event> findEventByEventCode(@Param("eventCode") String eventCode);
}
