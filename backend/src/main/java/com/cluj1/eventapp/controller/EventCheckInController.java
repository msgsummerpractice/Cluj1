package com.cluj1.eventapp.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cluj1.eventapp.dto.CheckInRequest;
import com.cluj1.eventapp.service.EventCheckInService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/events/checkin")
@RequiredArgsConstructor
public class EventCheckInController {
    private final EventCheckInService checkInService;

    @PostMapping
    public ResponseEntity<Void> checkIn(@Valid @RequestBody CheckInRequest request, Principal principal) {
        checkInService.processCheckIn(principal.getName(), request);
        return ResponseEntity.ok().build();
    }
}
