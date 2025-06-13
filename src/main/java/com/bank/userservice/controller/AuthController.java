package com.bank.userservice.controller;

import com.bank.userservice.dto.auth.AuthRequestDto;
import com.bank.userservice.dto.auth.AuthResponseDto;
import com.bank.userservice.dto.auth.RequestContext;
import com.bank.userservice.model.Log.ApplicationLog;
import com.bank.userservice.repository.Log.ApplicationLogRepository;
import com.bank.userservice.service.AuthService;
import com.bank.userservice.service.IntegrationLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final ApplicationLogRepository applicationLogRepository;
    private final IntegrationLogService integrationLogService;
    private final RequestContext requestContext;
    private final String loggerName = this.getClass().getName();

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody AuthRequestDto requestDto) {
        try {
            requestContext.setRequestDto(requestDto);
            MDC.put("rqid", String.valueOf(requestDto.getRqid()));
            log.info("Registration request received with rqid: {}", requestDto.getRqid());

            applicationLogRepository.save(new ApplicationLog(
                    "INFO",
                    "Registration request received with rqid: " + requestDto.getRqid(),
                    requestDto.getRqid(),
                    LocalDateTime.now(),
                    loggerName
            ));
            if (requestDto.getRegistrationData() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration data is empty");
            }

            Map<String, Object> response = authService.register(requestDto.getRegistrationData(), requestDto.getRqid());

            // Логируем интеграционное взаимодействие
            AuthResponseDto responseDto = integrationLogService.mapToAuthResponseDto(requestDto, response);
            integrationLogService.logInteraction(requestDto, responseDto);

            return ResponseEntity.ok(responseDto);
        } finally {
            MDC.remove("rqid");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto requestDto) {
        try {
            requestContext.setRequestDto(requestDto);
            MDC.put("rqid", String.valueOf(requestDto.getRqid()));

            log.info("Login attempt received with rqid: {}", requestDto.getRqid());

            applicationLogRepository.save(new ApplicationLog(
                    "INFO",
                    "Login attempt received with rqid: " + requestDto.getRqid(),
                    requestDto.getRqid(),
                    LocalDateTime.now(),
                    loggerName
            ));
            if (requestDto.getLoginData() == null) {

                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Login data is required");
            }

            Map<String, Object> response = authService.login(requestDto.getLoginData(), requestDto.getRqid());
            AuthResponseDto responseDto = integrationLogService.mapToAuthResponseDto(requestDto, response);

            // Логируем интеграционное взаимодействие
            integrationLogService.logInteraction(requestDto, responseDto);
            return ResponseEntity.ok(responseDto);
        } finally {
            MDC.remove("rqid");
        }

    }

}


