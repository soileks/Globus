package com.bank.userservice.service;

import com.bank.userservice.dto.LoginDto;
import com.bank.userservice.dto.RegistrationDto;
import com.bank.userservice.dto.UserResponseDto;
import com.bank.userservice.dto.auth.AuthResponseDto;

import com.bank.userservice.model.User;
import com.bank.userservice.model.log.enums.LogLevel;

import com.bank.userservice.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для {@link AuthService}.
 *
 * <p>Проверяет:
 * <ul>
 *   <li>Процесс регистрации новых пользователей</li>
 *   <li>Процесс аутентификации существующих пользователей</li>
 *   <li>Обработку ошибочных сценариев</li>
 *   <li>Логирование всех операций</li>
 *   <li>Интеграцию с другими сервисами (Email, Captcha)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaptchaService captchaService;
    @Mock
    private EmailService emailService;

    @Mock
    private ApplicationLogService applicationLogService;
    @Mock
    private IntegrationLogService integrationLogService;

    @InjectMocks
    private AuthService authService;

    /** Кодировщик паролей для тестов */
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    /**
     * Настройка тестового окружения перед каждым тестом.
     * Устанавливает кодировщик паролей через reflection.
     */
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
    }

    /**
     * Тест успешной регистрации пользователя.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Корректность возвращаемого DTO</li>
     *   <li>Вызов всех необходимых зависимостей:
     *     <ul>
     *       <li>Проверка CAPTCHA</li>
     *       <li>Поиск существующего пользователя</li>
     *       <li>Сохранение нового пользователя</li>
     *       <li>Отправка email подтверждения</li>
     *     </ul>
     *   </li>
     *   <li>Логирование успешной операции</li>
     * </ul>
     */
    @Test
    void register_ValidInput_ReturnsSuccessMessage() throws JsonProcessingException, MessagingException {
        // Подготовка тестовых данных
        RegistrationDto dto = createValidRegistrationDto();

        // Настройка поведения моков:
        // 1. Пользователь не существует
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());
        // 2. Имитируем сохранение пользователя
        User user = createTestUser();
        when(userRepository.save(any(User.class))).thenReturn(user);
        // Настройка интеграционного логгера
        AuthResponseDto mockResponse = new AuthResponseDto();
        mockResponse.setRqid(dto.getRqid());
        mockResponse.setStatusCode(HttpStatus.OK.value());
        mockResponse.setResponse(Map.of(
                "message", "Login successful",
                "user", new UserResponseDto(1L, "testuser", "test@example.com", LocalDateTime.now())
        ));
        when(integrationLogService.logInteraction(any())).thenReturn(mockResponse);

        // Вызов тестируемого метода
        AuthResponseDto result = authService.register(dto);

        assertNotNull(result);
        assertEquals(result.getRqid(), mockResponse.getRqid());

        UserResponseDto userDto = (UserResponseDto) result.getResponse().get("user");
        assertEquals(1L, userDto.getId());
        assertEquals("testuser", userDto.getUsername());

        // Проверка вызовов зависимостей:
        // 1. Проверка CAPTCHA была вызвана
        verify(captchaService).verifyCaptcha(dto);
        // 2. Проверка существования пользователя
        verify(userRepository).findByUsername(dto.getUsername());
        // 3. Сохранение пользователя
        verify(userRepository).save(any(User.class));
        // 4. Проверка отправки email
        verify(emailService).sendVerificationEmail(eq("test@example.com"), any(), eq("rqid"));

        // 5. Проверка что логирование было вызвано
        verify(applicationLogService).log(
                eq(LogLevel.INFO),
                contains("Starting registration process for user:"),
                eq("rqid"),
                anyString()
        );

        verify(applicationLogService).log(
                eq(LogLevel.INFO),
                contains("registered successfully"),
                eq("rqid"),
                anyString()
        );
    }

    /**
     * Тест регистрации существующего пользователя.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Выброс BadCredentialsException</li>
     *   <li>Логирование попытки повторной регистрации</li>
     * </ul>
     */
    @Test
    void register_ExistingUser_ThrowsException() {
        RegistrationDto dto = createValidRegistrationDto();
        // Настраиваем мок так, будто пользователь уже существует
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(createTestUser()));

        // Проверяем что выбрасывается исключение
        assertThrows(BadCredentialsException.class, () -> authService.register(dto));

        // Проверяем что ошибка была залогирована
        verify(applicationLogService, atLeastOnce())
                .log(eq(LogLevel.WARN),
                        contains("Registration attempt with existing verified credentials"),
                        eq("rqid"),
                        anyString());
    }

    /**
     * Тест регистрации с невалидными данными.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Выброс IllegalArgumentException при пустых полях</li>
     *   <li>Логирование ошибки валидации</li>
     * </ul>
     */
    @Test
    void register_NullFields_ThrowsException() {
        RegistrationDto dto = new RegistrationDto();
        dto.setRqid("rqid");
        // Проверяем что при пустых полях выбрасывается исключение
        assertThrows(IllegalArgumentException.class, () -> authService.register(dto));

        // Проверяем логирование ошибки
        verify(applicationLogService, atLeastOnce())
                .log(eq(LogLevel.ERROR), anyString(), eq("rqid"), anyString());
    }

    /**
     * Тест успешного входа в систему.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Корректность возвращаемых данных пользователя</li>
     *   <li>Отправку уведомления на email</li>
     *   <li>Логирование успешного входа</li>
     *   <li>Сравнение хешей паролей</li>
     * </ul>
     */
    @Test
    void login_ValidCredentials_ReturnsSuccess() throws JsonProcessingException {
        // Подготовка
        LoginDto dto = createValidLoginDto();

        User mockUser = createTestUser();

        // Настройка моков
        when(userRepository.findByEmail(dto.getEmail()))
                .thenReturn(Optional.of(mockUser));

        AuthResponseDto mockResponse = new AuthResponseDto();
        mockResponse.setRqid(dto.getRqid());
        mockResponse.setStatusCode(HttpStatus.OK.value());
        mockResponse.setResponse(Map.of(
                "message", "Login successful",
                "user", new UserResponseDto(1L, "testuser", "test@example.com", LocalDateTime.now())
        ));
        when(integrationLogService.logInteraction(any()))
                .thenReturn(mockResponse);

        // Вызов
        AuthResponseDto result = authService.login(dto);

        // Проверки
        assertNotNull(result);
        assertEquals("Login successful", result.getResponse().get("message"));

        UserResponseDto userDto = (UserResponseDto) result.getResponse().get("user");
        assertEquals(1L, userDto.getId());
        assertEquals("testuser", userDto.getUsername());

        verify(emailService).sendLoginNotification(eq("test@example.com"), eq("testuser"),
                eq("rqid"));

        // Проверка логирования
        verify(applicationLogService).log(
                eq(LogLevel.INFO),
                contains("Login attempt processing for:"),
                eq("rqid"),
                anyString()
        );

        verify(applicationLogService).log(
                eq(LogLevel.INFO),
                contains("logged in successfully"),
                eq("rqid"),
                anyString()
        );
    }

    /**
     * Тест входа несуществующего пользователя.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Выброс BadCredentialsException</li>
     *   <li>Логирование попытки входа</li>
     * </ul>
     */
    @Test
    void login_InvalidUsername_ThrowsException() {
        LoginDto dto = createValidLoginDto();

        when(userRepository.findByEmail(dto.getEmail()))
                .thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(dto));

        verify(applicationLogService, atLeastOnce()).log(
                eq(LogLevel.ERROR),
                contains("non-existent user"),
                eq("rqid"),
                anyString()
        );
    }

    /**
     * Тест входа с неверным паролем.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Выброс BadCredentialsException</li>
     *   <li>Логирование неудачной попытки входа</li>
     *   <li>Корректность сравнения паролей</li>
     * </ul>
     */
    @Test
    void login_InvalidPassword_ThrowsException() {
        LoginDto dto = createValidLoginDto();

        User mockUser = createTestUser();
        mockUser.setPassword(passwordEncoder.encode("wrongpassword"));


        when(userRepository.findByEmail(dto.getEmail()))
                .thenReturn(Optional.of(mockUser));


        assertThrows(BadCredentialsException.class, () -> authService.login(dto));

        verify(applicationLogService, atLeastOnce()).log(
                eq(LogLevel.ERROR),
                contains("Invalid password attempt"),
                eq("rqid"),
                anyString()
        );

    }

    /**
     * Тест входа с пустыми полями.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Выброс IllegalArgumentException</li>
     *   <li>Логирование ошибки валидации</li>
     * </ul>
     */
    @Test
    void login_NullFields_ThrowsException() {
        LoginDto dto = new LoginDto();
        dto.setRqid("rqid");

        // Проверяем что при пустых полях выбрасывается исключение
        assertThrows(IllegalArgumentException.class, () -> authService.login(dto));

        verify(applicationLogService, atLeastOnce()).log(
                eq(LogLevel.ERROR),
                contains("Incomplete login data provided for:"),
                eq("rqid"),
                anyString()
        );
    }

    /**
     * Создает тестового пользователя.
     *
     * <p>Параметры:
     * <ul>
     *   <li>ID = 1L</li>
     *   <li>Username = "testuser"</li>
     *   <li>Email = "test@example.com"</li>
     *   <li>Подтвержденный email</li>
     *   <li>Хеш пароля "password123"</li>
     * </ul>
     */
    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setCreatedAt(LocalDateTime.now());
        user.setEmailVerified(true);
        user.setPassword(passwordEncoder.encode("password123"));
        return user;
    }

    /**
     * Создает валидный DTO для регистрации.
     *
     * <p>Содержит:
     * <ul>
     *   <li>Username = "testuser"</li>
     *   <li>Email = "test@example.com"</li>
     *   <li>Password = "password123"</li>
     *   <li>Тип CAPTCHA = "recaptcha"</li>
     *   <li>Валидный токен reCAPTCHA</li>
     * </ul>
     */
    private RegistrationDto createValidRegistrationDto() {
        RegistrationDto dto = new RegistrationDto();
        dto.setRqid("rqid");
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setPassword("password123");
        dto.setVerificationType("recaptcha");
        dto.setRecaptchaToken("valid-token");
        return dto;
    }

    /**
     * Создает валидный DTO для входа.
     *
     * <p>Содержит:
     * <ul>
     *   <li>Email = "test@example.com"</li>
     *   <li>Password = "password123"</li>
     * </ul>
     */
    private LoginDto createValidLoginDto() {
        LoginDto dto = new LoginDto();
        dto.setRqid("rqid");
        dto.setEmail("test@example.com");
        dto.setPassword("password123");
        return dto;
    }
}