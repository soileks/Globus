package com.bank.userservice.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
/**
 * DTO для регистрации нового пользователя.
 */
@Data
public class RegistrationDto  {
    /** Идентификатор запроса */
    private String rqid;

    /** Имя пользователя (обязательное поле, 2-20 символов) */
    @NotBlank(message = "Имя пользователя не должно быть пустым")
    @Size(min = 2, max = 20, message = "Имя пользователя должно содержать от 2 до 20 символов")
    private String username;

    /** Email пользователя (обязательное поле, валидный email) */
    @Email(message = "Некорректный email")
    @NotBlank(message = "Поле email не должно быть пустым")
    private String email;

    /** Пароль (обязательное поле, 5-20 символов) */
    @NotBlank(message = "Пароль не должен быть пустым")
    @Size(min = 5, max = 20, message = "Пароль должен содержать от 5 до 20 символов")
    private String password;

    /** Тип капчи (обязательное поле) */
    @NotBlank(message = "Тип капчи не должен быть пустым")
    @Enumerated(EnumType.STRING)
    private String verificationType; // 'recaptcha' или 'math'

    /** Токен reCAPTCHA */
    private String recaptchaToken;

    /** Токен математической капчи */
    private String mathToken;
}
