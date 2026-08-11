package com.cluj1.eventapp.model;
 
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;
 
@Entity
@Table(name = "notifications")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
 
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
 
    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private OffsetDateTime sentAt;
}