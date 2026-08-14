package com.cluj1.eventapp.service;


import com.cluj1.eventapp.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;

    public int getRegistrationsPerUser(UUID userId) {
        return registrationRepository.countTotalRegistrationsPerUser(userId);
    }
}
