package com.cluj1.eventapp.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.exception.EmailAlreadyRegisteredException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.cluj1.eventapp.dto.UpdateRoleRequest;
import com.cluj1.eventapp.dto.UserDTO;
import com.cluj1.eventapp.service.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH,
        RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS })
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UserDTO>> getUsers(
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(userService.getAllUsers(search));
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
            @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(userService.updateUserRole(id, request.getRole()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserDTO> updateUserStatus(
            @PathVariable UUID id,
            @RequestParam Boolean isActive) {
        return ResponseEntity.ok(userService.updateUserStatus(id, isActive));
    }
}
