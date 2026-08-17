package com.cluj1.eventapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

import com.cluj1.eventapp.model.enums.UserLocation;

@Entity
@Table(name = "user_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ToString.Exclude
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

    @ToString.Exclude
    @Column(name = "profile_picture", columnDefinition = "BYTEA")
    private byte[] profilePicture;
}