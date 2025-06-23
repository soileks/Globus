package com.bank.userservice.dto.auth;
import lombok.Data;
import java.time.LocalDateTime;

import java.util.Map;

/**
 * Ответные данные
 */
@Data
public class AuthResponseDto {
    /** Идентификатор запроса */
    private String rqid;
    /** Идентификатор ответа */
    private String rsid;
    /** Код статуса */
    private Integer statusCode;
    /** Данные ответа */
    private Map<String, Object> response;
    /** Время и дата ответа */
    private LocalDateTime responseTime;
}

