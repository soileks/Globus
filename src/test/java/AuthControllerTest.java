import com.bank.userservice.controller.AuthController;
import com.bank.userservice.dto.LoginDto;
import com.bank.userservice.dto.RegistrationDto;
import com.bank.userservice.dto.UserResponseDto;
import com.bank.userservice.dto.auth.AuthRequestDto;
import com.bank.userservice.dto.auth.AuthResponseDto;
import com.bank.userservice.dto.auth.RequestContext;

import com.bank.userservice.model.Log.ApplicationLog;
import com.bank.userservice.repository.Log.ApplicationLogRepository;
import com.bank.userservice.service.AuthService;
import com.bank.userservice.service.IntegrationLogService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private ApplicationLogRepository applicationLogRepository;

    @Mock
    private IntegrationLogService integrationLogService;

    @Mock
    private RequestContext requestContext;

    @InjectMocks
    private AuthController authController;

    @Test
    void register_ValidInput_ReturnsOk() {
        // 1. Подготовка тестовых данных
        AuthRequestDto requestDto = new AuthRequestDto();
        requestDto.setRqid(1L);
        requestDto.setRegistrationData(createValidRegistrationDto());

        Map<String, Object> serviceResponse = Map.of(
                "message", "Registration successful",
                "user", new UserResponseDto()
        );

        AuthResponseDto expectedResponse = new AuthResponseDto();
        expectedResponse.setRqid(1L);
        expectedResponse.setRsid(123L);
        expectedResponse.setStatusCode(HttpStatus.OK.value());
        expectedResponse.setResponse(serviceResponse);
        expectedResponse.setResponseTime(LocalDateTime.now());

        // 2. Настройка моков
        when(authService.register(any(RegistrationDto.class), anyLong()))
                .thenReturn(serviceResponse);

        when(integrationLogService.mapToAuthResponseDto(any(AuthRequestDto.class), anyMap()))
                .thenReturn(expectedResponse);

        // 3. Вызов тестируемого метода
        ResponseEntity<AuthResponseDto> response = authController.register(requestDto);

        // 4. Проверки
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Registration successful", response.getBody().getResponse().get("message"));
        assertNotNull(response.getBody().getResponse().get("user"));

        // 5. Проверка вызовов зависимостей
        verify(authService).register(any(RegistrationDto.class), eq(1L));
        verify(integrationLogService).mapToAuthResponseDto(any(AuthRequestDto.class), anyMap());
        verify(integrationLogService).logInteraction(any(AuthRequestDto.class), any(AuthResponseDto.class));
        verify(requestContext).setRequestDto(any(AuthRequestDto.class));
        verify(applicationLogRepository, atLeastOnce()).save(any(ApplicationLog.class));
    }

    @Test
    void login_ValidInput_ReturnsOk() {
        // 1. Подготовка тестовых данных
        AuthRequestDto requestDto = new AuthRequestDto();
        requestDto.setRqid(1L);
        requestDto.setLoginData(createValidLoginDto());

        Map<String, Object> serviceResponse = Map.of(
                "message", "Login successful",
                "user", new UserResponseDto()
        );

        AuthResponseDto expectedResponse = new AuthResponseDto();
        expectedResponse.setRqid(1L);
        expectedResponse.setRsid(123L);
        expectedResponse.setStatusCode(HttpStatus.OK.value());
        expectedResponse.setResponse(serviceResponse);
        expectedResponse.setResponseTime(LocalDateTime.now());

        // 2. Настройка моков
        when(authService.login(any(LoginDto.class), anyLong()))
                .thenReturn(serviceResponse);

        when(integrationLogService.mapToAuthResponseDto(any(AuthRequestDto.class), anyMap()))
                .thenReturn(expectedResponse);

        // 3. Вызов тестируемого метода
        ResponseEntity<AuthResponseDto> response = authController.login(requestDto);

        // 4. Проверки
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Login successful", response.getBody().getResponse().get("message"));
        assertNotNull(response.getBody().getResponse().get("user"));

        // 5. Проверка вызовов зависимостей
        verify(authService).login(any(LoginDto.class), eq(1L));
        verify(integrationLogService).mapToAuthResponseDto(any(AuthRequestDto.class), anyMap());
        verify(integrationLogService).logInteraction(any(AuthRequestDto.class), any(AuthResponseDto.class));
        verify(requestContext).setRequestDto(any(AuthRequestDto.class));
        verify(applicationLogRepository, atLeastOnce()).save(any(ApplicationLog.class));
    }

    @Test
    void register_NullRegistrationData_ThrowsException() {
        AuthRequestDto requestDto = new AuthRequestDto();
        requestDto.setRqid(1L);
        requestDto.setRegistrationData(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authController.register(requestDto));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Registration data is empty", exception.getReason());
    }

    @Test
    void login_NullLoginData_ThrowsException() {
        AuthRequestDto requestDto = new AuthRequestDto();
        requestDto.setRqid(1L);
        requestDto.setLoginData(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authController.login(requestDto));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Login data is required", exception.getReason());
    }

    // Вспомогательные методы
    private RegistrationDto createValidRegistrationDto() {
        RegistrationDto dto = new RegistrationDto();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setPassword("password123");
        dto.setVerificationType("recaptcha");
        dto.setRecaptchaToken("valid-token");
        return dto;
    }

    private LoginDto createValidLoginDto() {
        LoginDto dto = new LoginDto();
        dto.setUsernameOrEmail("testuser");
        dto.setPassword("password123");
        return dto;
    }
}
