package com.cluj1.eventapp.controller;

import com.cluj1.eventapp.dto.AttendanceReportExcelRowDto;

import com.cluj1.eventapp.service.RegistrationService;
import com.cluj1.eventapp.repository.RegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/registration")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final RegistrationRepository registrationRepository;

    @GetMapping("/count")
    public ResponseEntity<Integer> getRegistrationsCount(Principal principal) {
        String email = principal.getName();
        int count = registrationService.getRegistrationsPerUserByEmail(email);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/api/events/{id}/attendance-report/preview")
    public List<AttendanceReportExcelRowDto> previewReport(@PathVariable UUID id) {
        return registrationRepository.findAttendanceReportRows(id);
    }
}