package com.cluj1.eventapp.repository;

import org.springframework.stereotype.Repository;
import java.util.UUID;

import com.cluj1.eventapp.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("SELECT COUNT(e) FROM Event e INNER JOIN Registration r ON e.id = r.event.id INNER JOIN User u ON r.user.id = :user_id WHERE e.eventStartDate > :now")
    int countUpcomingEventsForUsers(@Param("now")OffsetDateTime now, @Param("user_id")UUID user_id);

}
