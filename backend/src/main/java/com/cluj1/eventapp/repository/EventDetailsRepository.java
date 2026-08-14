package com.cluj1.eventapp.repository;

import org.springframework.stereotype.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cluj1.eventapp.model.EventDetails;

@Repository
public interface EventDetailsRepository extends JpaRepository<EventDetails, UUID> {

}
