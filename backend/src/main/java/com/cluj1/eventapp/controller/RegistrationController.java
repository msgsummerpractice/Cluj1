package com.cluj1.eventapp.controller;


import com.cluj1.eventapp.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/registration")
public class RegistrationController {

    private final RegistrationService registrationService;

    @GetMapping("/count")
    public ResponseEntity<Integer> getRegistrationsCount(Principal principal) {
        String email = principal.getName();
        int count = registrationService.getRegistrationsPerUserByEmail(email);
        return ResponseEntity.ok(count);
    }
}
