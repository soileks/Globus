import com.bank.userservice.dto.LoginDto;
import com.bank.userservice.dto.RegistrationDto;
import com.bank.userservice.model.Log.ApplicationLog;
import com.bank.userservice.model.User;
import com.bank.userservice.repository.Log.ApplicationLogRepository;
import com.bank.userservice.repository.UserRepository;
import com.bank.userservice.service.AuthService;
import com.bank.userservice.service.CaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private ApplicationLogRepository applicationLogRepository;

    @InjectMocks
    private AuthService authService;
    //@Mock
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    private final Long testRqid = 1L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "passwordEncoder",passwordEncoder);
    }

    @Test
    void register_ValidInput_ReturnsSuccessMessage() {
        RegistrationDto dto = createValidRegistrationDto();

        when(userRepository.existsByUsernameOrEmail(anyString(), anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        var result = authService.register(dto, testRqid);

        assertNotNull(result);
        assertEquals("Registration successful", result.get("message"));
        assertNotNull(result.get("user"));

        verify(captchaService).verifyCaptcha(dto, testRqid);
        verify(userRepository).existsByUsernameOrEmail(dto.getUsername(), dto.getEmail());
        verify(userRepository).save(any(User.class));
        verify(applicationLogRepository, atLeast(5)).save(any(ApplicationLog.class));
    }

    @Test
    void register_ExistingUser_ThrowsException() {
        RegistrationDto dto = createValidRegistrationDto();

        when(userRepository.existsByUsernameOrEmail(anyString(), anyString())).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> authService.register(dto, testRqid));

        verify(applicationLogRepository).save(argThat(log ->
                log.getMessage().contains("existing credentials")));
    }

    @Test
    void register_NullFields_ThrowsException() {
        RegistrationDto dto = new RegistrationDto();

        assertThrows(IllegalArgumentException.class, () -> authService.register(dto, testRqid));

        verify(applicationLogRepository).save(argThat(log ->
                log.getMessage().contains("Incomplete registration data")));
    }

    @Test
    void login_ValidCredentials_ReturnsSuccess() {
        LoginDto dto = createValidLoginDto();
        User mockUser = createTestUser();

        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.of(mockUser));

        var result = authService.login(dto, testRqid);

        assertNotNull(result);
        assertEquals("Login successful", result.get("message"));
        assertNotNull(result.get("user"));

        verify(applicationLogRepository).save(argThat(log ->
                log.getMessage().contains("logged in successfully")));
    }

    @Test
    void login_InvalidUsername_ThrowsException() {
        LoginDto dto = createValidLoginDto();

        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(dto, testRqid));

        verify(applicationLogRepository).save(argThat(log ->
                log.getMessage().contains("non-existent user")));
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        LoginDto dto = createValidLoginDto();
        User mockUser = createTestUser();
        mockUser.setPassword(passwordEncoder.encode("wrongpassword"));

        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.of(mockUser));

        assertThrows(BadCredentialsException.class, () -> authService.login(dto, testRqid));

        verify(applicationLogRepository).save(argThat(log ->
                log.getMessage().contains("Invalid password attempt")));
    }

    @Test
    void login_NullFields_ThrowsException() {
        LoginDto dto = new LoginDto();

        assertThrows(IllegalArgumentException.class, () -> authService.login(dto, testRqid));
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        return user;
    }

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