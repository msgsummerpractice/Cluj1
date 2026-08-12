package com.cluj1.eventapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "transportation_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportationDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false, unique = true)
    private Registration registration;

    @Column(name = "driver_name", nullable = false)
    private String driverName;

    @Column(name = "driver_phone_number", nullable = false, length = 50)
    private String driverPhoneNumber;
}