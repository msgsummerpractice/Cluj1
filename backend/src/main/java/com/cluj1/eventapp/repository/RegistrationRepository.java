package com.cluj1.eventapp.repository;

import com.cluj1.eventapp.dto.AttendanceReportExcelRowDto;
import com.cluj1.eventapp.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.user.id = :userId")
    int countTotalRegistrationsPerUser(@Param("userId") UUID userId);

    Optional<Registration> findByUserIdAndEventId(UUID userId, UUID eventId);

    @Query("""
            SELECT new com.cluj1.eventapp.dto.AttendanceReportExcelRowDto(
                ud.lastName,
                ud.firstName,
                u.email,
                r.gdprConsent,
                r.registrationDate,
                CASE WHEN ar.id IS NOT NULL THEN true ELSE false END
            )
            FROM Registration r
            JOIN r.user u
            JOIN u.userDetails ud
            LEFT JOIN AttendanceRecord ar ON ar.registration = r
            WHERE r.event.id = :eventId
            ORDER BY ud.lastName, ud.firstName
            """)
    List<AttendanceReportExcelRowDto> findAttendanceReportRows(@Param("eventId") UUID eventId);
}
