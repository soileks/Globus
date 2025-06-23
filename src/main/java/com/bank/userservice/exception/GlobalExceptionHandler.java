package com.bank.userservice.exception;

import com.bank.userservice.dto.auth.AuthResponseDto;

import com.bank.userservice.service.ApplicationLogService;
import com.bank.userservice.service.IntegrationLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


import java.io.IOException;

import java.util.Map;

import java.util.stream.Collectors;

import static com.bank.userservice.model.log.enums.LogLevel.ERROR;
/**
 * Глобальный обработчик исключений для REST API.
 *
 * <p>Обрабатывает все необработанные исключения в приложении и возвращает
 * стандартизированные JSON-ответы. Для каждого типа исключений:
 * <ul>
 *   <li>Логирует ошибку в интеграционные логи</li>
 *   <li>Формирует стандартный ответ с деталями ошибки</li>
 *   <li>Возвращает соответствующий HTTP статус</li>
 * </ul>
 */
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    private final ApplicationLogService applicationLogService;
    private final IntegrationLogService integrationLogService;
    private final String loggerName = this.getClass().getName();

    /**
     * Обрабатывает ошибки аутентификации (неверные учетные данные).
     *
     * @param ex исключение BadCredentialsException
     * @return ResponseEntity с деталями ошибки и статусом 401 (Unauthorized)
     * @throws IOException при ошибках ввода-вывода
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) throws IOException {


        AuthResponseDto authResponseDto = integrationLogService
                .logErrorToIntegrationLogs(HttpStatus.UNAUTHORIZED, ex.getMessage());

        return new ResponseEntity<>(Map.of("errorDetails", authResponseDto), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Обрабатывает ошибки валидации входных данных.
     *
     * @param ex исключение MethodArgumentNotValidException
     * @return ResponseEntity с деталями ошибки и статусом 400 (Bad Request)
     * @throws JsonProcessingException при ошибках обработки JSON
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) throws JsonProcessingException {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        AuthResponseDto authResponseDto = integrationLogService
                .logErrorToIntegrationLogs(HttpStatus.BAD_REQUEST, errorMessage);

        return new ResponseEntity<>(Map.of("errorDetails", authResponseDto), HttpStatus.BAD_REQUEST);
    }

    /**
     * Обрабатывает ошибки формата чисел в математической капче.
     *
     * @param ex исключение NumberFormatException
     * @return ResponseEntity с деталями ошибки и статусом 400 (Bad Request)
     * @throws JsonProcessingException при ошибках обработки JSON
     */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Map<String, Object>> handleNumberFormatException(NumberFormatException ex) throws JsonProcessingException {

        AuthResponseDto authResponseDto = integrationLogService.logErrorToIntegrationLogs(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );

        return new ResponseEntity<>(Map.of("errorDetails", authResponseDto), HttpStatus.BAD_REQUEST);
    }

    /**
     * Обрабатывает ошибки сериализации/десериализации JSON.
     *
     * @param ex исключение JsonProcessingException
     * @return ResponseEntity с деталями ошибки и статусом 500 (Internal Server Error)
     * @throws JsonProcessingException при ошибках обработки JSON
     */
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleJsonProcessingException(JsonProcessingException ex) throws JsonProcessingException {

        AuthResponseDto authResponseDto = integrationLogService.logErrorToIntegrationLogs(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error (JSON processing)"
        );

        applicationLogService.log(ERROR,
                "JSON processing error: " + ex.getMessage(),
                authResponseDto.getRqid(),
                loggerName);

        return new ResponseEntity<>(Map.of("errorDetails", authResponseDto),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Обрабатывает ошибки некорректных аргументов и NPE.
     *
     * @param ex исключение (IllegalArgumentException или NullPointerException)
     * @return ResponseEntity с деталями ошибки и статусом 400 (Bad Request)
     * @throws JsonProcessingException при ошибках обработки JSON
     */
    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
    public ResponseEntity<Map<String, Object>> handlePasswordEncodingErrors(RuntimeException ex) throws JsonProcessingException {

        AuthResponseDto authResponseDto = integrationLogService
                .logErrorToIntegrationLogs(HttpStatus.BAD_REQUEST, ex.getMessage());

        return new ResponseEntity<>(Map.of("errorDetails", authResponseDto), HttpStatus.BAD_REQUEST);
    }

    /**
     * Обрабатывает ошибки с капчей.
     *
     * @param ex исключение InvalidCaptchaException
     * @return ResponseEntity с деталями ошибки и статусом 400 (Bad Request)
     * @throws JsonProcessingException при ошибках обработки JSON
     */
    @ExceptionHandler(InvalidCaptchaException.class)
    public ResponseEntity<Map<String, Object>> handleCaptchaVerificationException(InvalidCaptchaException ex) throws JsonProcessingException {

        AuthResponseDto authResponseDto = integrationLogService.logErrorToIntegrationLogs(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );

        return new ResponseEntity<>(Map.of("errorDetails", authResponseDto), HttpStatus.BAD_REQUEST);
    }
    /**
     * Обрабатывает ошибки неподтвержденного email.
     *
     * @param ex исключение EmailNotVerifiedException
     * @return ResponseEntity с деталями ошибки и статусом 400 (Bad Request)
     * @throws JsonProcessingException при ошибках обработки JSON
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException ex) throws JsonProcessingException {
        AuthResponseDto authResponseDto = integrationLogService.logErrorToIntegrationLogs(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );

        return new ResponseEntity<>(Map.of("errorDetails", authResponseDto), HttpStatus.BAD_REQUEST);
    }
    /**
     * Обрабатывает ошибки истекшего срока регистрации.
     *
     * @param ex исключение AccountExpiredException
     * @return ResponseEntity с деталями ошибки и статусом 400 (Bad Request)
     * @throws JsonProcessingException при ошибках обработки JSON
     */
    @ExceptionHandler(AccountExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(AccountExpiredException ex) throws JsonProcessingException {
        AuthResponseDto authResponseDto = integrationLogService.logErrorToIntegrationLogs(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );

        return new ResponseEntity<>(Map.of("errorDetails", authResponseDto), HttpStatus.BAD_REQUEST);
    }
    /**
     * Обрабатывает все неперехваченные Runtime исключения.
     *
     * @param ex неперехваченное исключение RuntimeException
     * @return ResponseEntity с деталями ошибки и статусом 500 (Internal Server Error)
     * @throws JsonProcessingException при ошибках обработки JSON
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleAllUncaughtException(RuntimeException ex) throws JsonProcessingException {

        AuthResponseDto authResponseDto = integrationLogService
                .logErrorToIntegrationLogs(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());

        return new ResponseEntity<>(Map.of("errorDetails", authResponseDto),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

}