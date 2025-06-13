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

class LoginDtoTest {

    private final Validator validator;

    public LoginDtoTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validLoginDto_NoViolations() {
        LoginDto dto = new LoginDto();
        dto.setUsernameOrEmail("user@example.com");
        dto.setPassword("password123");

        Set<ConstraintViolation<LoginDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource // автоматически добавляет два параметра в тест: null и ""
    @MethodSource("blankStrings")
    void invalidUsernameOrEmail_ViolationsPresent(String username) {
        LoginDto dto = new LoginDto();
        dto.setUsernameOrEmail(username);
        dto.setPassword("password123");

        Set<ConstraintViolation<LoginDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Имя пользователя или email не должно быть пустым",
                violations.iterator().next().getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @MethodSource("blankStrings")
    void invalidPassword_ViolationsPresent(String password) {
        LoginDto dto = new LoginDto();
        dto.setUsernameOrEmail("user@example.com");
        dto.setPassword(password);

        Set<ConstraintViolation<LoginDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
    }

    private static Stream<String> blankStrings() {
        return Stream.of( " ", "  ", "   ");
    }
}