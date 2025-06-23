import com.bank.userservice.controller.AuthController;
import com.bank.userservice.dto.LoginDto;
import com.bank.userservice.dto.RegistrationDto;
import com.bank.userservice.dto.UserResponseDto;
import com.bank.userservice.dto.auth.AuthResponseDto;

import com.bank.userservice.dto.auth.RequestContext;

import com.bank.userservice.service.AuthService;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/**
 * Тестовый класс для {@link AuthController}.
 *
 * <p>Проверяет:
 * <ul>
 *   <li>Корректность обработки запросов регистрации и входа</li>
 *   <li>Правильность управления контекстом (MDC и RequestContext)</li>
 *   <li>Обработку ошибок сервисного слоя</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock
    private AuthService authService;
    @Mock
    private RequestContext requestContext;

    @InjectMocks
    private AuthController authController;

    /** Тестовый идентификатор запроса */
    private final String testRqid = "test-rqid-123";
    /** Тестовый email пользователя */
    private final String testEmail = "testuser@example.com";
    /** Тестовое имя пользователя */
    private final String testUsername = "testuser";

    /**
     * Очистка MDC после каждого теста.
     */
    @AfterEach
    void tearDown() {
        MDC.clear();
    }
    /**
     * Проверяет успешную регистрацию пользователя.
     *
     * <p>Тест проверяет:
     * <ul>
     *   <li>Корректность HTTP статуса ответа (200 OK)</li>
     *   <li>Правильность заполнения контекста запроса</li>
     *   <li>Очистку MDC после выполнения</li>
     *   <li>Содержимое успешного ответа</li>
     * </ul>
     */
    @Test
    void register_ValidInput_ReturnsOk() throws JsonProcessingException, MessagingException {
        // 1. Подготовка
        RegistrationDto requestDto = createValidRegistrationDto();
        AuthResponseDto mockResponse = createSuccessAuthResponse("Registration successful");
        // Настройка моков
        when(authService.register(requestDto)).thenReturn(mockResponse);

        // 2. Вызов
        ResponseEntity<AuthResponseDto> response = authController.register(requestDto);

        // 3. Проверки
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testRqid, response.getBody().getRqid());
        assertEquals("Registration successful",
                response.getBody().getResponse().get("message"));

        verify(requestContext).setRqid(testRqid);
        assertNull(MDC.get("rqid"));
    }
    /**
     * Проверяет успешный вход пользователя.
     *
     * <p>Тест проверяет:
     * <ul>
     *   <li>Корректность HTTP статуса ответа (200 OK)</li>
     *   <li>Правильность заполнения контекста запроса</li>
     *   <li>Содержимое успешного ответа</li>
     * </ul>
     */
    @Test
    void login_ValidInput_ReturnsOk() throws JsonProcessingException {
        // 1. Подготовка
        LoginDto requestDto = createValidLoginDto();
        AuthResponseDto mockResponse = createSuccessAuthResponse("Login successful");
        // Настройка моков
        when(authService.login(requestDto)).thenReturn(mockResponse);

        // 2. Вызов
        ResponseEntity<AuthResponseDto> response = authController.login(requestDto);

        // 3. Проверки
        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(testRqid, response.getBody().getRqid());
        assertEquals("Login successful",
                response.getBody().getResponse().get("message"));

        verify(requestContext).setRqid(testRqid);
        assertNull(MDC.get("rqid"));
    }
    /**
     * Проверяет обработку ошибок при регистрации.
     *
     */
    @Test
    void register_ServiceThrowsException_CleansContext() throws JsonProcessingException, MessagingException {
        // 1. Подготовка
        RegistrationDto requestDto = createValidRegistrationDto();

        // 2. Настройка моков для генерации ошибки
        when(authService.register(requestDto))
                .thenThrow(new RuntimeException("Service error"));

        // 3. Проверка исключения
        assertThrows(RuntimeException.class,
                () -> authController.register(requestDto));

        verify(requestContext).setRqid(testRqid);
        assertNull(MDC.get("rqid"));
    }

    /**
     * Создает успешный AuthResponseDto для тестирования.
     *
     * @param message сообщение для ответа
     * @return подготовленный AuthResponseDto
     */
    private AuthResponseDto createSuccessAuthResponse(String message) {
        AuthResponseDto response = new AuthResponseDto();
        response.setRqid(testRqid);
        response.setStatusCode(HttpStatus.OK.value());
        response.setResponse(Map.of(
                "message", message,
                "user", new UserResponseDto(1L, testUsername, testEmail, LocalDateTime.now())
        ));
        return response;
    }
    /**
     * Создает валидный RegistrationDto для тестирования.
     *
     * @return подготовленный RegistrationDto
     */
    private RegistrationDto createValidRegistrationDto() {
        RegistrationDto dto = new RegistrationDto();
        dto.setRqid(testRqid);
        dto.setUsername(testUsername);
        dto.setEmail(testEmail);
        dto.setPassword("ValidPass123!");
        dto.setVerificationType("recaptcha");
        dto.setRecaptchaToken("valid-token");
        return dto;
    }
    /**
     * Создает валидный LoginDto для тестирования.
     *
     * @return подготовленный LoginDto
     */
    private LoginDto createValidLoginDto() {
        LoginDto dto = new LoginDto();
        dto.setRqid(testRqid);
        dto.setEmail(testEmail);
        dto.setPassword("ValidPass123!");
        return dto;
    }
}
