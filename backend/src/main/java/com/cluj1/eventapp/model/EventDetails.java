package com.cluj1.eventapp.model;
 
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
 
@Entity
@Table(name = "event_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;
 
    @Column(columnDefinition = "TEXT")
    private String description;
 
    @Lob
    private byte[] poster;
 
    @Column(name = "food_provided")
    private Boolean foodProvided;
 
    @Column(name = "qr_code_content", columnDefinition = "TEXT")
    private String qrCodeContent;
 
    @Column(name = "event_code", length = 6, unique = true)
    private String eventCode;
}