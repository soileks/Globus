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
/**
 * Тестовый класс для проверки валидации {@link RegistrationDto}.
 *
 * <p>Использует параметризованные тесты для проверки всех сценариев валидации.
 */
class RegistrationDtoTest {
    /** Валидатор для проверки объектов. */
    private final Validator validator;
    /**
     * Конструктор класса RegistrationDtoTest.
     * Инициализирует валидатор, используя стандартную фабрику валидаторов.
     */
    public RegistrationDtoTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    /**
     * Проверяет валидный DTO.
     *
     * <p>Убеждается, что корректно заполненный DTO не вызывает нарушений валидации.
     */
    @Test
    void validRegistrationDto_NoViolations() {
        RegistrationDto dto = createValidDto();

        Set<ConstraintViolation<RegistrationDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }
    /**
     * Параметризованный тест для проверки валидации username.
     *
     * @param username тестовые значения:
     *                <ul>
     *                  <li>null и пустая строка</li>
     *                  <li>пробельные строки</li>
     *                </ul>
     */
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
    /**
     * Параметризованный тест для проверки валидации email.
     *
     * @param email тестовые значения:
     *             <ul>
     *               <li>null и пустая строка</li>
     *               <li>невалидные форматы email</li>
     *             </ul>
     */
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
    /**
     * Параметризованный тест для проверки валидации password.
     *
     * @param password тестовые значения:
     *                <ul>
     *                  <li>null и пустая строка</li>
     *                  <li>слишком короткие пароли</li>
     *                </ul>
     */
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
    /**
     * Параметризованный тест для проверки валидации verificationType.
     *
     * @param verificationType тестовые значения:
     *                        <ul>
     *                          <li>null и пустая строка</li>
     *                        </ul>
     */
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
    /**
     * Создает валидный DTO для тестирования.
     *
     * @return корректно заполненный {@link RegistrationDto}
     */
    private RegistrationDto createValidDto() {
        RegistrationDto dto = new RegistrationDto();
        dto.setUsername("validuser");
        dto.setEmail("valid@example.com");
        dto.setPassword("validpass123");
        dto.setVerificationType("recaptcha");
        return dto;
    }
    /**
     * Предоставляет набор пробельных строк для тестирования.
     *
     * @return поток пробельных строк разной длины
     */
    private static Stream<String> blankStrings() {
        return Stream.of("   ", " ", "  ");
    }
}
