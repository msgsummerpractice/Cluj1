package com.cluj1.eventapp.dto;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AttendanceRecordDto {
    private UUID id;
    private OffsetDateTime checkInTime;
    private UserBasicInfoDto user;

    @Data
    @Builder
    public static class UserBasicInfoDto {
        private String firstName;
        private String lastName;
    }
}