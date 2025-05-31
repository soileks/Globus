package com.bank.userservice.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Data
public class RegistrationDto {
    @NotBlank @Size(min = 2, max = 50)
    private String username;

    @Email @NotBlank
    private String email;

    @NotBlank @Size(min = 5, max = 100)
    private String password;
}
