package com.cluj1.eventapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder @AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
}