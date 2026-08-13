package com.cluj1.eventapp.repository;

import com.cluj1.eventapp.model.UserDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, UUID> {
    Optional<byte[]> findProfilePictureByUserId(UUID userId);
    Optional<UserDetails> findById(UUID userId);
}
