package com.cluj1.eventapp.mapper;

import com.cluj1.eventapp.dto.AttendanceRecordDto;
import com.cluj1.eventapp.model.AttendanceRecord;
import org.springframework.stereotype.Component;

@Component
public class AttendanceRecordMapper {
    public AttendanceRecordDto mapToDto(AttendanceRecord record) {
        return AttendanceRecordDto.builder()
                .id(record.getId())
                .checkInTime(record.getCheckInTime())
                .user(AttendanceRecordDto.UserBasicInfoDto.builder()
                        .firstName(record.getRegistration().getUser().getUserDetails().getFirstName())
                        .lastName(record.getRegistration().getUser().getUserDetails().getLastName())
                        .build())
                .build();
    }
}
