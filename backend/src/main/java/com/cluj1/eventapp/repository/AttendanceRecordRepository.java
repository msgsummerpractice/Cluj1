package com.cluj1.eventapp.repository;

import com.cluj1.eventapp.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    boolean existsByRegistrationId(UUID registrationId);
}
