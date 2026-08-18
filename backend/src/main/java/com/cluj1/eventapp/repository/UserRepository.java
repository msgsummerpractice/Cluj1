package com.cluj1.eventapp.repository;

import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.Role;

import java.util.UUID;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Query(value = "SELECT u FROM User u LEFT JOIN FETCH u.userDetails ud WHERE " +
            ":search IS NULL OR :search = '' OR " +
            "LOWER(ud.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(ud.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(u.role AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(ud.location AS string)) LIKE LOWER(CONCAT('%', :search, '%'))", countQuery = "SELECT COUNT(u) FROM User u LEFT JOIN u.userDetails ud WHERE "
                    +
                    ":search IS NULL OR :search = '' OR " +
                    "LOWER(ud.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(ud.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(CAST(u.role AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(CAST(ud.location AS string)) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    boolean existsByEmail(String email);

    long countByRoleAndIsActiveTrue(Role role);
    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash WHERE u.id = :userId")
    void updatePasswordHash(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash);
}