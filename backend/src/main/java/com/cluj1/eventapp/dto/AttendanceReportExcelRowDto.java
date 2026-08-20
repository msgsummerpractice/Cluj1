package com.cluj1.eventapp.dto;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceReportExcelRowDto {
    private String lastName;
    private String firstName;
    private String email;
    private boolean hasGdprConsent;
    private OffsetDateTime registrationDate;
    private boolean isPresent;
}
