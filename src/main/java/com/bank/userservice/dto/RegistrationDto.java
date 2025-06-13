package com.bank.userservice.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Data
public class RegistrationDto {
    @NotBlank(message = "Имя пользователя не должно быть пустым")
    @Size(min = 2, max = 20, message = "Имя пользователя должно содержать от 2 до 20 символов")
    private String username;

    @Email(message = "Некорректный email")
    @NotBlank(message = "Поле email не должно быть пустым")
    private String email;

    @NotBlank(message = "Пароль не должен быть пустым")
    @Size(min = 5, max = 20, message = "Пароль должен содержать от 5 до 20 символов")
    private String password;
    @NotBlank(message = "Тип капчи не должен быть пустым")
    private String verificationType; // 'recaptcha' или 'math'

    private String recaptchaToken;

    private String mathProblem; // задача

    private String mathAnswer; // ответ пользователя к задаче

}
