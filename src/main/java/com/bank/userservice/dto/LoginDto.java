package com.bank.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDto {
    @NotBlank(message = "Имя пользователя или email не должно быть пустым")
    private String usernameOrEmail;  // Можно вводить и то, и другое
    @NotBlank
    private String password;
}
