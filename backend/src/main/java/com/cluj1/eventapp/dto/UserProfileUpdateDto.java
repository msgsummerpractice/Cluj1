package com.cluj1.eventapp.dto;

import com.cluj1.eventapp.model.enums.UserLocation;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Builder
public class UserProfileUpdateDto {
    private UserLocation userLocation;
    private MultipartFile profilePicture;
}
