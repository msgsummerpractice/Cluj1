package com.cluj1.eventapp.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInCodesDto {
    private String qrCodeContent;
    private String eventCode;
}
