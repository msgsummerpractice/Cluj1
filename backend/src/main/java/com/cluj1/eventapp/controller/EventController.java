package com.cluj1.eventapp.controller;


import com.cluj1.eventapp.service.EventService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    @GetMapping("/countRegistrationPerUser")
    public ResponseEntity<Integer> getRegistrationCountPerUser(Principal principal){
        String email = principal.getName();
        return ResponseEntity.ok(eventService.getUpcomingRegisteredEventsCountPerUserByEmail(email));
    }
}

