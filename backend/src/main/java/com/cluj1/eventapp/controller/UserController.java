package com.cluj1.eventapp.controller;

import java.util.UUID;

import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.dto.UpdateRoleRequest;
import com.cluj1.eventapp.dto.UserDTO;
import com.cluj1.eventapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<UserDTO>> getUsers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getAllUsers(search, PageRequest.of(page, size)));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(@RequestBody @Valid UserRegistrationDto userRegistrationDto) {
        if (!userRegistrationDto.getPassword().equals(userRegistrationDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        userService.registerUser(userRegistrationDto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateRoleRequest request) {
        return ResponseEntity.ok(userService.updateUserRole(id, request.getRole()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserDTO> updateUserStatus(
            @PathVariable UUID id,
            @RequestParam boolean isActive) {
        return ResponseEntity.ok(userService.updateUserStatus(id, isActive));
    }
}