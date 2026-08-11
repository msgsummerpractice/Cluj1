package com.cluj1.eventapp.model;

import com.cluj1.eventapp.model.enums.CheckInMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "attendance_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false, unique = true)
    private Registration registration;

    @CreationTimestamp
    @Column(name = "check_in_time", updatable = false)
    private OffsetDateTime checkInTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_in_method", nullable = false, length = 50)
    private CheckInMethod checkInMethod;
}