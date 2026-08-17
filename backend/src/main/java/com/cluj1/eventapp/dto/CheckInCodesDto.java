package com.cluj1.eventapp.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInCodesDto {
    private String qrCodeContent;
    private String eventCode;
}
