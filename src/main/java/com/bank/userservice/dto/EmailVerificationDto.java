package com.bank.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
/**
 * DTO для подтверждения email.
 */
@AllArgsConstructor
@Data
public class EmailVerificationDto {
    /** Идентификатор запроса */
    private String rqid;

    /** Токен верификации (обязательное поле) */
    @NotBlank
    private String token;

    /** Email пользователя (обязательное поле) */
    @NotBlank
    private String email;
}
