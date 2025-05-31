package com.bank.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// DTO для ответа
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private UserResponseDto user;

    public AuthResponse(String token, UserResponseDto user) {
        this.token = token;
        this.user = user;
    }
}
