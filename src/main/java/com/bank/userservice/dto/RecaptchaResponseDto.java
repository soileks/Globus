package com.bank.userservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecaptchaResponseDto {
    private boolean success;
    private String hostname;
    private float score;
    private String action;
    private List<String> errorCodes;
}
