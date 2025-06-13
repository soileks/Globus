package com.bank.userservice.exception;

import com.bank.userservice.dto.auth.AuthRequestDto;
import com.bank.userservice.dto.auth.AuthResponseDto;
import com.bank.userservice.dto.auth.RequestContext;
import com.bank.userservice.model.Log.ApplicationLog;
import com.bank.userservice.repository.Log.ApplicationLogRepository;

import com.bank.userservice.service.IntegrationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    private  final ApplicationLogRepository applicationLogRepository;
    private final RequestContext requestContext;
    private final IntegrationLogService integrationLogService;
    private final String loggerName = this.getClass().getName();

    // Для проверки капчи / существования пользователя
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        // Получаем rqid из MDC
        //Long rqid = Long.parseLong(MDC.get("rqid"));
        AuthRequestDto requestDto = requestContext.getRequestDto();
        Long rqid = requestDto.getRqid();
        log.error("Handling ResponseStatusException [rqid: {}]: {} - {}",
                rqid, ex.getStatusCode(), ex.getReason());

        applicationLogRepository.save(new ApplicationLog(
                "ERROR",
                "ResponseStatusException: " + ex.getReason() + " [status: " + ex.getStatusCode() + "]",
                rqid,
                LocalDateTime.now(),
                loggerName
        ));

        AuthResponseDto authResponseDto = integrationLogService.logErrorToIntegrationLogs(requestDto, ex.getStatusCode(), ex.getReason());

        Map<String, Object> errorDetails = new HashMap<>();
        //errorDetails.put("error", ex.getReason());
       // errorDetails.put("status", String.valueOf(ex.getStatusCode().value()));
        errorDetails.put("errorDetails", authResponseDto);
        return new ResponseEntity<>(errorDetails, ex.getStatusCode());
    }
    // Обработка ошибок аутентификации
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) throws IOException {
        //Long rqid = Long.parseLong(MDC.get("rqid"));
        AuthRequestDto requestDto = requestContext.getRequestDto();
        Long rqid = requestDto.getRqid();
        log.error("Authentication failed [rqid: {}]: {}", rqid, ex.getMessage());

        applicationLogRepository.save(new ApplicationLog(
                "ERROR",
                "Authentication failed: " + ex.getMessage(),
                        rqid,
                LocalDateTime.now(),
                loggerName
        ));

        AuthResponseDto authResponseDto = integrationLogService.logErrorToIntegrationLogs(requestDto, HttpStatus.UNAUTHORIZED, "Invalid username or password");

        Map<String, Object> errorDetails = new HashMap<>();
        //errorDetails.put("error", "Invalid username or password");
        //errorDetails.put("status", String.valueOf(HttpStatus.UNAUTHORIZED.value()));
        errorDetails.put("errorDetails", authResponseDto);
        return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED);
    }
    // Обработка ошибок валидации
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        //Long rqid = Long.parseLong(MDC.get("rqid"));
        AuthRequestDto requestDto = requestContext.getRequestDto();
        Long rqid = requestDto.getRqid();

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.error("Validation error [rqid: {}]: {}", rqid, errorMessage);

        applicationLogRepository.save(new ApplicationLog(
                "ERROR",
                "Validation error: " + errorMessage,
                rqid,
                LocalDateTime.now(),
                loggerName
        ));
        AuthResponseDto authResponseDto = integrationLogService.logErrorToIntegrationLogs(requestDto, HttpStatus.BAD_REQUEST, errorMessage);
        Map<String, Object> errorDetails = new HashMap<>();
        //errorDetails.put("error", "Validation error");
        //errorDetails.put("message", ex.getMessage());
        //errorDetails.put("status", String.valueOf(HttpStatus.BAD_REQUEST.value()));
        errorDetails.put("errorDetails", authResponseDto);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    // Обработка ошибок Null при регистрации и входе
    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
    public ResponseEntity<Map<String, Object>> handlePasswordEncodingErrors(RuntimeException ex) {
        //Long rqid = Long.parseLong(MDC.get("rqid"));

        AuthRequestDto requestDto = requestContext.getRequestDto();
        Long rqid = requestDto.getRqid();
        log.error("Invalid input data [rqid: {}]: {}", rqid, ex.getMessage());

        applicationLogRepository.save(new ApplicationLog(
                "ERROR",
                "Invalid input data: " + ex.getMessage(),
                rqid,
                LocalDateTime.now(),
                loggerName
        ));
        AuthResponseDto authResponseDto = integrationLogService.logErrorToIntegrationLogs(requestDto, HttpStatus.BAD_REQUEST, ex.getMessage());
        Map<String, Object> errorDetails = new HashMap<>();
        //errorDetails.put("error", "Incorrectly entered data");
        //errorDetails.put("message", ex.getMessage());
       // errorDetails.put("status", String.valueOf(HttpStatus.BAD_REQUEST.value()));
        errorDetails.put("errorDetails", authResponseDto);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleAllUncaughtException(RuntimeException ex) {
        //Long rqid = Long.parseLong(MDC.get("rqid"));
        AuthRequestDto requestDto = requestContext.getRequestDto();
        Long rqid = requestDto.getRqid();

        log.error("Unexpected error occurred [rqid: {}]: {}", rqid, ex.getMessage(), ex);

        applicationLogRepository.save(new ApplicationLog(
                "ERROR",
                "Unexpected error: " + ex.getMessage(),
                rqid,
                LocalDateTime.now(),
                loggerName
        ));
        AuthResponseDto authResponseDto = integrationLogService.logErrorToIntegrationLogs(requestDto, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        Map<String, Object> errorDetails = new HashMap<>();
        //errorDetails.put("error", "An unexpected error occurred");
       // errorDetails.put("status", String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
        errorDetails.put("errorDetails", authResponseDto);
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}