package com.bank.userservice.dto.auth;
import lombok.Data;
import java.time.LocalDateTime;

import java.util.Map;

@Data
public class AuthResponseDto {
    private Long rqid;
    private Long rsid;
    private Integer statusCode;
    private Map<String, Object> response;
    private LocalDateTime responseTime;
}

