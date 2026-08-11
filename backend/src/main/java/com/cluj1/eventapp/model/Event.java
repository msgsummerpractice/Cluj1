package com.cluj1.eventapp.model;
 
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @Column(nullable = false)
    private String name;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventLocation location;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventType type;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;
 
    @Column(name = "event_start_date")
    private OffsetDateTime eventStartDate;
 
    @Column(name = "event_end_time")
    private OffsetDateTime eventEndTime;
 
    @Column(name = "registration_end_date")
    private OffsetDateTime registrationEndDate;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
 
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
 
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @OneToOne(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private EventDetails eventDetails;
}