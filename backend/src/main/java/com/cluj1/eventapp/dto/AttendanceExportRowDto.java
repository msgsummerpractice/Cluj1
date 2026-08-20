package com.cluj1.eventapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceExportRowDto {
    private int nrCrt;
    private String lastName;
    private String firstName;
    private String eventName;
    private String email;
    private String foodPreference;
    private String transportRequired;
    private String accommodationRequired;
    private String driverName;
    private String driverPhoneNumber;
    private String gdpr;
}