package com.cluj1.eventapp.controller;

import com.cluj1.eventapp.dto.AuthResponse;
import com.cluj1.eventapp.dto.LogInRequest;

import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.exception.EmailAlreadyRegisteredException;
import com.cluj1.eventapp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity registerUser(@RequestBody @Valid UserRegistrationDto userRegistrationDto){
        if(!userRegistrationDto.getPassword().equals(userRegistrationDto.getConfirmPassword())){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Passwords do not match");
        }
        try{
            authService.registerUser(userRegistrationDto);
            return ResponseEntity.ok().build();
        }catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }catch(EmailAlreadyRegisteredException e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LogInRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}