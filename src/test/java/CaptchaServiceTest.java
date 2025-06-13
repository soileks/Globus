
import com.bank.userservice.dto.RecaptchaResponseDto;
import com.bank.userservice.dto.RegistrationDto;
import com.bank.userservice.model.Log.ApplicationLog;
import com.bank.userservice.repository.Log.ApplicationLogRepository;
import com.bank.userservice.service.CaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ApplicationLogRepository applicationLogRepository;
    @InjectMocks
    private CaptchaService captchaService;

    private RegistrationDto registrationDto;


    @BeforeEach
    void setUp() {
        captchaService.setRestTemplate(restTemplate);
       // captchaService.setSecretKey("test-secret-key");
        registrationDto = new RegistrationDto();
    }

    // Нужно подумать как лучше сделать тесты с reCaptcha
    @Test
    void verifyCaptcha_RecaptchaInvalid_ThrowsException() {
        registrationDto.setVerificationType("recaptcha");
        registrationDto.setRecaptchaToken("invalid-token");

        RecaptchaResponseDto mockResponse = new RecaptchaResponseDto();
        mockResponse.setSuccess(false);

        when(restTemplate.postForObject(
                eq("https://www.google.com/recaptcha/api/siteverify"),
                any(),
                eq(RecaptchaResponseDto.class)
        )).thenReturn(mockResponse);

        assertThrows(ResponseStatusException.class,
                () -> captchaService.verifyCaptcha(registrationDto, 1L));
        verify(applicationLogRepository, atLeastOnce()).save(any(ApplicationLog.class));
    }

    @Test
    void verifyCaptcha_RecaptchaValid_NoException() {
        registrationDto.setVerificationType("recaptcha");
        registrationDto.setRecaptchaToken("valid-token");

        RecaptchaResponseDto mockResponse = new RecaptchaResponseDto();
        mockResponse.setSuccess(true);

        when(restTemplate.postForObject(anyString(), any(), eq(RecaptchaResponseDto.class)))
                .thenReturn(mockResponse);

        assertDoesNotThrow(() -> captchaService.verifyCaptcha(registrationDto, 1L));
    }

    @Test
    void verifyCaptcha_MathValid_NoException() {
        registrationDto.setVerificationType("math");
        registrationDto.setMathProblem("2 + 3");
        registrationDto.setMathAnswer("5");

        assertDoesNotThrow(() -> captchaService.verifyCaptcha(registrationDto, 1L));

    }

    @Test
    void verifyCaptcha_MathInvalid_ThrowsException() {
        registrationDto.setVerificationType("math");
        registrationDto.setMathProblem("2 + 3");
        registrationDto.setMathAnswer("6");

        assertThrows(ResponseStatusException.class,
                () -> captchaService.verifyCaptcha(registrationDto, 1L));
    }

    @Test
    void verifyCaptcha_InvalidType_ThrowsException() {
        registrationDto.setVerificationType("invalid-type");

        assertThrows(ResponseStatusException.class,
                () -> captchaService.verifyCaptcha(registrationDto, 1L));
    }

    @Test
    void verifyMathCaptcha_ValidAddition_ReturnsTrue() {
        assertTrue(captchaService.verifyMathCaptcha("2 + 3", "5", 1L));
    }

    @Test
    void verifyMathCaptcha_ValidSubtraction_ReturnsTrue() {
        assertTrue(captchaService.verifyMathCaptcha("5 - 3", "2", 1L));
    }

    @Test
    void verifyMathCaptcha_ValidMultiplication_ReturnsTrue() {
        assertTrue(captchaService.verifyMathCaptcha("2 * 3", "6", 1L));
    }

    @Test
    void verifyMathCaptcha_InvalidAddition_ReturnsFalse() {
        assertFalse(captchaService.verifyMathCaptcha("2 + 3", "6", 1L));
    }

    @Test
    void verifyMathCaptcha_InvalidFormat_ThrowsException() {
        assertThrows(ResponseStatusException.class,
                () -> captchaService.verifyMathCaptcha("2 + abc", "xyz", 1L));
    }

    @Test
    void verifyMathCaptcha_NullProblem_ThrowsException() {
        assertThrows(ResponseStatusException.class,
                () -> captchaService.verifyMathCaptcha(null, "5", 1L));
    }

    @Test
    void verifyMathCaptcha_NullAnswer_ThrowsException() {
        assertThrows(ResponseStatusException.class,
                () -> captchaService.verifyMathCaptcha("2 + 3", null, 1L));
    }

}
