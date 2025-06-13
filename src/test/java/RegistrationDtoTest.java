import com.bank.userservice.dto.RegistrationDto;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;


import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationDtoTest {

    private final Validator validator;

    public RegistrationDtoTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validRegistrationDto_NoViolations() {
        RegistrationDto dto = createValidDto();

        Set<ConstraintViolation<RegistrationDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource  // null и ""
    @MethodSource("blankStrings")  // " " и "  " (разные варианты пробелов)
    @ValueSource(strings = {"a", "verylongusernameexceedingtwentychars"})  // Конкретные значения для проверки длины
    void invalidUsername_ViolationsPresent(String username) {
        RegistrationDto dto = createValidDto();
        dto.setUsername(username);

        Set<ConstraintViolation<RegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());

        String message = violations.iterator().next().getMessage();
        assertTrue(message.contains("не должно быть пустым") ||
                message.contains("от 2 до 20 символов"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @MethodSource("blankStrings")
    @ValueSource(strings = {"invalid", "user@", "@domain.com"})
    void invalidEmail_ViolationsPresent(String email) {
        RegistrationDto dto = createValidDto();
        dto.setEmail(email);

        Set<ConstraintViolation<RegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());

        String message = violations.iterator().next().getMessage();
        assertTrue(message.contains("не должно быть пустым") ||
                message.contains("Некорректный email"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @MethodSource("blankStrings")
    @ValueSource(strings = {"123", "pw"})
    void invalidPassword_ViolationsPresent(String password) {
        RegistrationDto dto = createValidDto();
        dto.setPassword(password);

        Set<ConstraintViolation<RegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());

        String message = violations.iterator().next().getMessage();
        assertTrue(message.contains("не должен быть пустым") ||
                message.contains("от 5 до 20 символов"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @MethodSource("blankStrings")
    void invalidVerificationType_ViolationsPresent(String verificationType) {
        RegistrationDto dto = createValidDto();
        dto.setVerificationType(verificationType);

        Set<ConstraintViolation<RegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals("Тип капчи не должен быть пустым",
                violations.iterator().next().getMessage());
    }

    private RegistrationDto createValidDto() {
        RegistrationDto dto = new RegistrationDto();
        dto.setUsername("validuser");
        dto.setEmail("valid@example.com");
        dto.setPassword("validpass123");
        dto.setVerificationType("recaptcha");
        return dto;
    }

    private static Stream<String> blankStrings() {
        return Stream.of("   ", " ", "  ");
    }
}
