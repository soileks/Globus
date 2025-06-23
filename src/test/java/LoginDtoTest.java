import com.bank.userservice.dto.LoginDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
/**
 * Тестовый класс для проверки валидации {@link LoginDto}.
 */
class LoginDtoTest {
    /** Валидатор для проверки объектов. */
    private final Validator validator;
    /**
     * Конструктор класса RLoginDtoTest.
     * Инициализирует валидатор, используя стандартную фабрику валидаторов.
     */
    public LoginDtoTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    /**
     * Проверяет валидный DTO.
     *
     * <p>Убеждается, что корректно заполненный DTO не вызывает нарушений валидации.
     */
    @Test
    void validLoginDto_NoViolations() {
        LoginDto dto = new LoginDto();
        dto.setEmail("user@example.com");
        dto.setPassword("password123");

        Set<ConstraintViolation<LoginDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }
    /**
     * Параметризованный тест невалидного usernameOrEmail
     * @param email Тестовые значения (null, "", " ", "  ")
     */
    @ParameterizedTest
    @NullAndEmptySource // автоматически добавляет два параметра в тест: null и ""
    @MethodSource("blankStrings")
    void invalidUsernameOrEmail_ViolationsPresent(String email) {
        LoginDto dto = new LoginDto();
        dto.setEmail(email);
        dto.setPassword("password123");

        Set<ConstraintViolation<LoginDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("email не должен быть пустым",
                violations.iterator().next().getMessage());
    }
    /**
     * Параметризованный тест невалидного password
     * @param password Тестовые значения (null, "", " ", "  ")
     */
    @ParameterizedTest
    @NullAndEmptySource
    @MethodSource("blankStrings")
    void invalidPassword_ViolationsPresent(String password) {
        LoginDto dto = new LoginDto();
        dto.setEmail("user@example.com");
        dto.setPassword(password);

        Set<ConstraintViolation<LoginDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
    }
    /**
     * Предоставляет набор пробельных строк для тестирования.
     *
     * @return поток пробельных строк разной длины
     */
    private static Stream<String> blankStrings() {
        return Stream.of( " ", "  ", "   ");
    }
}