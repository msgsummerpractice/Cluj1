package com.cluj1.eventapp.repository;

import com.cluj1.eventapp.model.AttendanceRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    boolean existsByRegistrationId(UUID registrationId);

    @Query("SELECT a FROM AttendanceRecord a WHERE a.registration.event.id = :eventId ORDER BY a.checkInTime DESC")
    List<AttendanceRecord> findRecentByEventId(@Param("eventId") UUID eventId, Pageable pageable);
}
