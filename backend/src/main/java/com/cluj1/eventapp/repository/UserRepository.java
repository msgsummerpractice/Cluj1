package com.cluj1.eventapp.repository;

import com.cluj1.eventapp.model.User;
import java.util.UUID;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userDetails ud WHERE " +
            ":search IS NULL OR :search = '' OR " +
            "LOWER(ud.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(ud.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(u.role AS String)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(ud.location AS String)) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<User> searchUsers(@Param("search") String search);

    boolean existsByEmail(String email);
}
