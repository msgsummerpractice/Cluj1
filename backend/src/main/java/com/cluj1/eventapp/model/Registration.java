package com.cluj1.eventapp.model;
 
import com.cluj1.eventapp.model.enums.FoodPreference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;
 
@Entity
@Table(
    name = "registrations",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "event_id"})}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
 
    @Column(name = "gdpr_consent", nullable = false)
    @Builder.Default
    private Boolean gdprConsent = false;
 
    @Column(name = "photo_consent", nullable = false)
    @Builder.Default
    private Boolean photoConsent = false;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "food_preference", length = 50)
    private FoodPreference foodPreference;
 
    @Column(name = "transportation_needed", nullable = false)
    @Builder.Default
    private Boolean transportationNeeded = false;
 
    @Column(name = "accommodation_needed", nullable = false)
    @Builder.Default
    private Boolean accommodationNeeded = false;
 
    @Column(name = "accommodation_days")
    private Integer accommodationDays;
 
    @CreationTimestamp
    @Column(name = "registration_date", updatable = false)
    private OffsetDateTime registrationDate;
    @OneToOne(mappedBy = "registration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TransportationDetails transportationDetails;
    @OneToOne(mappedBy = "registration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AttendanceRecord attendanceRecord;
}