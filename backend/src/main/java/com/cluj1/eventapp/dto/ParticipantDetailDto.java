package com.cluj1.eventapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParticipantDetailDto {
    private String name;
    private String email;
    private String status;
    private String checkInTime;
}