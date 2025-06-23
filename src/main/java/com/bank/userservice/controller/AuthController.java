package com.bank.userservice.controller;

import com.bank.userservice.dto.EmailVerificationDto;
import com.bank.userservice.dto.LoginDto;
import com.bank.userservice.dto.RegistrationDto;
import com.bank.userservice.dto.auth.AuthResponseDto;

import com.bank.userservice.dto.auth.RequestContext;
import com.bank.userservice.service.AuthService;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Контроллер для обработки запросов аутентификации.
 *
 * <p>Обрабатывает:
 * <ul>
 *   <li>Регистрацию новых пользователей</li>
 *   <li>Аутентификацию существующих пользователей</li>
 *   <li>Подтверждение email адресов</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final RequestContext requestContext;

    /**
     * Регистрирует нового пользователя в системе.
     *
     * @param registrationDto DTO с данными регистрации
     * @return AuthResponseDto с результатом регистрации
     * @throws JsonProcessingException при ошибках обработки JSON
     * @throws MessagingException      при ошибках отправки email подтверждения
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegistrationDto registrationDto) throws JsonProcessingException, MessagingException {
        try {
            //requestContext.setRequestDto(requestDto);
            MDC.put("rqid", String.valueOf(registrationDto.getRqid()));
            requestContext.setRqid(registrationDto.getRqid());

            AuthResponseDto responseDto = authService.register(registrationDto);

            return ResponseEntity.ok(responseDto);
        } finally {
            MDC.remove("rqid");
        }
    }

    /**
     * Аутентифицирует пользователя в системе.
     *
     * @param requestDto DTO с данными входа
     * @return AuthResponseDto с данными аутентифицированного пользователя
     * @throws JsonProcessingException при ошибках обработки JSON
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginDto requestDto) throws JsonProcessingException {
        try {
            //requestContext.setRequestDto(requestDto);
            MDC.put("rqid", String.valueOf(requestDto.getRqid()));
            requestContext.setRqid(requestDto.getRqid());

            AuthResponseDto responseDto = authService.login(requestDto);

            return ResponseEntity.ok(responseDto);
        } finally {
            MDC.remove("rqid");
        }

    }

    /**
     * Подтверждает email пользователя и перенаправляет на страницу успеха.
     *
     * @param email    email для подтверждения
     * @param token    верификационный токен
     * @param rqid     идентификатор запроса
     * @param response объект HttpServletResponse для перенаправления
     * @return перенаправление на страницу /email-verified
     * @throws IOException при ошибках перенаправления
     */
    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(
            @RequestParam String email,
            @RequestParam String token,
            @RequestParam String rqid,
            HttpServletResponse response) throws IOException {

        try {
            MDC.put("rqid", rqid);
            requestContext.setRqid(rqid);

            EmailVerificationDto dto = new EmailVerificationDto(rqid, token, email);
            authService.verifyEmail(dto);

            // Перенаправляем на страницу успеха
            response.sendRedirect("http://localhost:8080/email-verified");
            return null;
        } finally {
            MDC.remove("rqid");
        }
    }
}


