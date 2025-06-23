package com.bank.userservice.service;

import com.bank.userservice.dto.RecaptchaResponseDto;
import com.bank.userservice.dto.RegistrationDto;
import com.bank.userservice.exception.InvalidCaptchaException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для {@link CaptchaService}.
 *
 * <p>Проверяет:
 * <ul>
 *   <li>Валидацию reCAPTCHA через Google API</li>
 *   <li>Проверку математической CAPTCHA</li>
 *   <li>Обработку неверных типов CAPTCHA</li>
 *   <li>Граничные случаи и ошибочные сценарии</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ApplicationLogService applicationLogService;
    @InjectMocks
    private CaptchaService captchaService;

    /** DTO для тестирования регистрации */
    private RegistrationDto registrationDto;

    /**
     * Настройка тестового окружения перед каждым тестом.
     * Инициализирует restTemplate и тестовые данные.
     */
    @BeforeEach
    void setUp() {
        captchaService.setRestTemplate(restTemplate);
       // captchaService.setSecretKey("test-secret-key");
        registrationDto = new RegistrationDto();
        registrationDto.setRqid("rqid");
    }

    /**
     * Тест неудачной проверки reCAPTCHA.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Ответ сервиса с success=false</li>
     *   <li>Выброс исключения InvalidCaptchaException</li>
     *   <li>Корректность параметров запроса к API</li>
     * </ul>
     */
    @Test
    void verifyCaptcha_RecaptchaInvalid_ThrowsException() {
        registrationDto.setVerificationType("recaptcha");
        registrationDto.setRecaptchaToken("invalid-token");

        // Настраиваем мок ответа от сервиса reCAPTCHA
        RecaptchaResponseDto mockResponse = new RecaptchaResponseDto();
        mockResponse.setSuccess(false);

        when(restTemplate.postForObject(
                eq("https://www.google.com/recaptcha/api/siteverify"),
                any(),
                eq(RecaptchaResponseDto.class)
        )).thenReturn(mockResponse);

        // Проверяем что выбрасывается исключение
        assertThrows(InvalidCaptchaException.class,
                () -> captchaService.verifyCaptcha(registrationDto));

    }

    /**
     * Тест успешной проверки reCAPTCHA.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Ответ сервиса с success=true</li>
     *   <li>Отсутствие исключений при валидном токене</li>
     *   <li>Корректность работы с API</li>
     * </ul>
     */
    @Test
    void verifyCaptcha_RecaptchaValid_NoException() {
        registrationDto.setVerificationType("recaptcha");
        registrationDto.setRecaptchaToken("valid-token");

        RecaptchaResponseDto mockResponse = new RecaptchaResponseDto();
        mockResponse.setSuccess(true);

        when(restTemplate.postForObject(anyString(), any(), eq(RecaptchaResponseDto.class)))
                .thenReturn(mockResponse);

        // Проверяем что исключение не выбрасывается
        assertDoesNotThrow(() -> captchaService.verifyCaptcha(registrationDto));
    }

    /**
     * Тест успешной проверки математической капчи.
     *
     */
    @Test
    void verifyCaptcha_MathValid_NoException() {
        registrationDto.setVerificationType("math");
        registrationDto.setMathToken("2 + 3 = 5");

        // Проверяем что исключение не выбрасывается
        assertDoesNotThrow(() -> captchaService.verifyCaptcha(registrationDto));
    }

    /**
     * Тест неверного ответа на математическую CAPTCHA.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Выброс исключения при неверном ответе</li>
     * </ul>
     */
    @Test
    void verifyCaptcha_MathInvalid_ThrowsException() {
        registrationDto.setVerificationType("math");
        registrationDto.setMathToken("2 + 3 = 6");

        // Проверяем что выбрасывается исключение
        assertThrows(InvalidCaptchaException.class,
                () -> captchaService.verifyCaptcha(registrationDto));
    }

    /**
     * Тест неверного типа капча.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Выброс исключения при неизвестном типе</li>
     * </ul>
     */
    @Test
    void verifyCaptcha_InvalidType_ThrowsException() {
        registrationDto.setVerificationType("invalid-type");

        // Проверяем что выбрасывается исключение
        assertThrows(InvalidCaptchaException.class,
                () -> captchaService.verifyCaptcha(registrationDto));
    }

    /**
     * Тест корректного сложения.
     *
     */

    @Test
    void verifyMathCaptcha_ValidAddition_ReturnsTrue() {
        assertTrue(captchaService.verifyMathCaptcha("2 + 3 = 5", "rqid"));
    }
    /**
     * Тест корректного вычитания.
     *
     */
    @Test
    void verifyMathCaptcha_ValidSubtraction_ReturnsTrue() {
        assertTrue(captchaService.verifyMathCaptcha("5 - 3 = 2", "rqid"));
    }
    /**
     * Тест корректного умножения.
     *
     */
    @Test
    void verifyMathCaptcha_ValidMultiplication_ReturnsTrue() {
        assertTrue(captchaService.verifyMathCaptcha("2 * 3 = 6", "rqid"));
    }
    /**
     * Тест некорректного ответа сложения.
     *
     */
    @Test
    void verifyMathCaptcha_InvalidAddition_ReturnsFalse() {
        assertFalse(captchaService.verifyMathCaptcha("2 + 3 = 6",  "rqid"));
    }
    /**
     * Тест неверного формата уравнения.
      */
    @Test
    void verifyMathCaptcha_InvalidFormat_ThrowsException() {
        assertThrows(NumberFormatException.class,
                () -> captchaService.verifyMathCaptcha("2 + abc = xys", "rqid"));
    }
    /**
     * Тест на null вместо токена.
      */
    @Test
    void verifyMathCaptcha_NullProblem_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> captchaService.verifyMathCaptcha(null, "rqid"));
    }
    /**
     * Тест пустого ответа.
     */
    @Test
    void verifyMathCaptcha_NullAnswer_ThrowsException() {
        assertThrows(InvalidCaptchaException.class,
                () -> captchaService.verifyMathCaptcha("2 + 3 = ", "rqid"));
    }

}
