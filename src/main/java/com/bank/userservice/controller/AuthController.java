package com.bank.userservice.controller;

import com.bank.userservice.dto.AuthResponse;
import com.bank.userservice.dto.LoginDto;
import com.bank.userservice.dto.RegistrationDto;
import com.bank.userservice.model.User;
import com.bank.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bank.userservice.security.JwtUtils;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationDto dto) {
        User user = userService.registerUser(dto);

        // Автоматическая аутентификация после регистрации
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );

        String token = jwtUtils.generateJwtToken(auth);
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + token)
                .body("Registration successful");
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginDto dto) {
        // AuthenticationManager автоматически использует наш UserDetailsService
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.getUsernameOrEmail(), // Передаём строку (username/email)
                        dto.getPassword()
                )
        );

        // Генерация токена
        String token = jwtUtils.generateJwtToken(auth);

        // Получаем данные пользователя для ответа
        User user = userService.findByUsernameOrEmail(dto.getUsernameOrEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + token)
                .body(new AuthResponse(token, user.toDto()));
    }
}