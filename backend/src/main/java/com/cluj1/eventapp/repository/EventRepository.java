package com.cluj1.eventapp.repository;

import org.springframework.stereotype.Repository;
import java.util.UUID;

import com.cluj1.eventapp.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

}
