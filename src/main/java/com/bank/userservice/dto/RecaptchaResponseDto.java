package com.bank.userservice.dto;

import lombok.Data;

import java.util.List;

/**
 * DTO для ответа reCAPTCHA.
 */
@Data
public class RecaptchaResponseDto {
    /** Успешность проверки */
    private boolean success;
    /** Имя хоста */
    private String hostname;
    /** Оценка (score) */
    private float score;
    /** Действие */
    private String action;
    /** Коды ошибок */
    private List<String> errorCodes;
}
