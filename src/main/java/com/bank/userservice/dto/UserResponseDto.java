package com.bank.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для ответа с данными пользователя.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    /** ID пользователя */
    private Long id;
    /** Имя пользователя */
    private String username;
    /** Email пользователя */
    private String email;
    /** Дата создания (формат yyyy-MM-dd'T'HH:mm:ss) */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}