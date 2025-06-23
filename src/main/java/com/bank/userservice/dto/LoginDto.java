package com.bank.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 * DTO для входа пользователя в систему.
 */
@Data
public class LoginDto  {
    /** Идентификатор запроса */
    private String rqid;

    /** Email пользователя (обязательное поле) */
    @NotBlank(message = "email не должен быть пустым")
    private String email;

    /** Пароль пользователя (обязательное поле) */
    @NotBlank
    private String password;
}
