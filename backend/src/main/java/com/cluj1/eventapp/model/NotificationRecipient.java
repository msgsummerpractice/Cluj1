package com.cluj1.eventapp.model;
 
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
 
@Entity
@Table(
    name = "notification_recipients",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"notification_id", "user_id"})}
)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationRecipient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}