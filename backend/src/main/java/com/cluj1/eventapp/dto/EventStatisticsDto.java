package com.cluj1.eventapp.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class EventStatisticsDto {
    private int invitedCount;
    private int registrationCount;
    private long participantCount;
    private Map<String, Long> registrationTimeDistribution;
    private Map<String, Double> foodPreferencePercentages;
    private double accommodationPercentage;
    private double transportPercentage;
    private double photoConsentPercentage;

    private List<ParticipantDetailDto> participants;
}