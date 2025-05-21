package com.bank.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// класс для регистрации клиентов
@Data
public class UserRegistrationDto {
    @NotBlank
    @Size(min = 2, max = 50)
    private String username;

    @NotBlank
    @Size(min = 5, max = 100)
    private String password;
}