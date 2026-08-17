package com.cluj1.eventapp.repository;

import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.EventDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventDetailsRepository extends JpaRepository<EventDetails, UUID> {
    EventDetails findByEvent(Event event);
}
