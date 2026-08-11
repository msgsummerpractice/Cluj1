package com.cluj1.eventapp.model;
 
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

import com.cluj1.eventapp.model.enums.UserLocation;
 
@Entity
@Table(name = "user_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
 
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
 
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserLocation location;
 
    @Lob
    @Column(name = "profile_picture")
    private byte[] profilePicture;
}