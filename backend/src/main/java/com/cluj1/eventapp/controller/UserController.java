package com.cluj1.eventapp.controller;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cluj1.eventapp.dto.UserProfileDto;
import com.cluj1.eventapp.dto.UserProfileUpdateDto;
import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.exception.EmailAlreadyRegisteredException;
import com.cluj1.eventapp.model.enums.UserLocation;
import jakarta.validation.Valid;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.cluj1.eventapp.dto.UserDTO;
import com.cluj1.eventapp.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final UserService userService;

    private static final List<String> ALLOWED_CONTENT_TYPE = Arrays.asList("image/jepg", "image/png");

    Tika tika = new Tika();

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UserDTO>> getUsers(
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(userService.getAllUsers(search));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(@RequestBody @Valid UserRegistrationDto userRegistrationDto){
        if(!userRegistrationDto.getPassword().equals(userRegistrationDto.getConfirmPassword())){
            throw new IllegalArgumentException("Passwords do not match");
        }

        userService.registerUser(userRegistrationDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getUserProfile(Principal principal){
        String email = principal.getName();
        UserProfileDto userProfile = userService.getUserProfileByEmail(email);
        return ResponseEntity.ok(userProfile);
    }

    @PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateUserProfile(Principal principal,
                                                  @RequestParam(value = "userLocation", required = false)UserLocation userLocation,
                                                  @RequestParam (value = "profilePicture", required = false) MultipartFile profilePicture) {

        try{
            if(profilePicture != null && !profilePicture.isEmpty()){
                try(InputStream inputStream = profilePicture.getInputStream()){
                    String trueFileType = tika.detect(inputStream);

                    if(!ALLOWED_CONTENT_TYPE.contains(trueFileType)){
                        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Image should be in PNG or JPEG format!");
                    }
                }
            }
            String email = principal.getName();
            userService.updateUserProfile(email, UserProfileUpdateDto.builder()
                    .userLocation(userLocation)
                    .profilePicture(profilePicture)
                    .build());
            return ResponseEntity.ok().build();
        }catch(IOException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
