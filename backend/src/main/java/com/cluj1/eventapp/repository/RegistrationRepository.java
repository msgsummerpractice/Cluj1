package com.cluj1.eventapp.repository;

import com.cluj1.eventapp.dto.AttendanceReportExcelRowDto;
import com.cluj1.eventapp.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

        @Query("SELECT COUNT(r) FROM Registration r WHERE r.user.id = :userId")
        int countTotalRegistrationsPerUser(@Param("userId") UUID userId);

        Optional<Registration> findByEventIdAndUserId(UUID eventId, UUID userId);
        
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

        @Query("SELECT r FROM Registration r WHERE r.user.id = :userId AND r.event.id IN :eventIds")
        List<Registration> findByUserIdAndEventIdIn(@Param("userId") UUID userId,
                        @Param("eventIds") Collection<UUID> eventIds);

        @Query("SELECT r FROM Registration r " +
                        "JOIN FETCH r.user u " +
                        "JOIN FETCH u.userDetails ud " +
                        "LEFT JOIN FETCH r.transportationDetails td " +
                        "JOIN FETCH r.event e " +
                        "WHERE r.event.id = :eventId " +
                        "ORDER BY ud.lastName ASC, ud.firstName ASC")
        List<Registration> findAllByEventIdWithDetails(@Param("eventId") UUID eventId);

    List<Registration> findByEventId(UUID eventId);
}