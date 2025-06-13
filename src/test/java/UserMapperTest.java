import com.bank.userservice.dto.UserResponseDto;
import com.bank.userservice.mapper.UserMapper;
import com.bank.userservice.model.User;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    @Test
    void userToUserResponseDto_ValidInput_CorrectMapping() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);

        UserResponseDto dto = UserMapper.usertoUserResponseDto(user);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("testuser", dto.getUsername());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    void userToUserResponseDto_NullInput_ThrowsException() {
        assertThrows(NullPointerException.class, () -> UserMapper.usertoUserResponseDto(null));
    }
}