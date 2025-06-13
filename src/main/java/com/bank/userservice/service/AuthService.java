package com.bank.userservice.service;

import com.bank.userservice.dto.LoginDto;
import com.bank.userservice.dto.RegistrationDto;
import com.bank.userservice.model.Log.ApplicationLog;
import com.bank.userservice.model.User;
import com.bank.userservice.mapper.UserMapper;
import com.bank.userservice.repository.Log.ApplicationLogRepository;
import com.bank.userservice.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Data
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final UserRepository userRepository;
    private final ApplicationLogRepository applicationLogRepository;
    private final String loggerName = this.getClass().getName();


    @Transactional
    public Map<String, Object> register(RegistrationDto dto, Long rqid) {
        log.info("Starting registration process for user: {}", dto.getUsername());

        applicationLogRepository.save(new ApplicationLog(
                "INFO",
                "Starting registration process for user: " + dto.getUsername(),
                rqid,
                LocalDateTime.now(),
                loggerName
        ));
            captchaService.verifyCaptcha(dto, rqid);
            User user = registerUser(dto, rqid);

            log.info("User {} registered successfully", dto.getUsername());
            applicationLogRepository.save(new ApplicationLog(
                    "INFO",
                    "User " + dto.getUsername() + " registered successfully " ,
                    rqid,
                    LocalDateTime.now(),
                    loggerName
            ));
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Registration successful");
            response.put("user", UserMapper.usertoUserResponseDto(user));
            return response;
    }

    public User registerUser(RegistrationDto registrationDto, Long rqid) {
        log.debug("Checking if user {} or email {} already exists",
                registrationDto.getUsername(), registrationDto.getEmail());

        applicationLogRepository.save(new ApplicationLog(
                "DEBUG",
                "Checking if user " + registrationDto.getUsername() + " or email " + registrationDto.getUsername() + " already exists",
                rqid,
                LocalDateTime.now(),
                loggerName
        ));
        // Проверка существования такого пользователя в БД
        if (userRepository.existsByUsernameOrEmail(registrationDto.getUsername(), registrationDto.getEmail())) {
            log.warn("Registration attempt with existing username {} or email {}",
                    registrationDto.getUsername(), registrationDto.getEmail());

            applicationLogRepository.save(new ApplicationLog(
                    "WARN",
                    "Registration attempt with existing credentials",
                    rqid,
                    LocalDateTime.now(),
                    loggerName
            ));

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists" );
        }

        // Проверка введенных данных на null
        if(registrationDto.getUsername() == null || registrationDto.getPassword() == null || registrationDto.getEmail() == null){
            log.error("Incomplete registration data provided for username: {}", registrationDto.getUsername());

            applicationLogRepository.save(new ApplicationLog(
                    "ERROR",
                    "Incomplete registration data",
                    rqid,
                    LocalDateTime.now(),
                    loggerName
            ));

            throw new IllegalArgumentException("Registration data is incorrect");
        }

        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        log.debug("Encoding password for user {}", registrationDto.getUsername());
        applicationLogRepository.save(new ApplicationLog(
                "DEBUG",
                "Encoding password for user:" + registrationDto.getUsername(),
                rqid,
                LocalDateTime.now(),
                loggerName
        ));
        // Шифрование пароля
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        // Сохранение пользователя
        User savedUser = userRepository.save(user);

        log.debug("User {} successfully saved to database with ID: {}",
                savedUser.getUsername(), savedUser.getId());

        applicationLogRepository.save(new ApplicationLog(
                "DEBUG",
                "User registered successfully",
                rqid,
                LocalDateTime.now(),
                loggerName
        ));

        return savedUser;
    }

    public Map<String, Object> login(LoginDto dto, Long rqid) {

        log.debug("Login attempt processing for {}", dto.getUsernameOrEmail());

        applicationLogRepository.save(new ApplicationLog(
                "DEBUG",
                "Login attempt processing for: " + dto.getUsernameOrEmail(),
                rqid,
                LocalDateTime.now(),
                loggerName
        ));

            if(dto.getUsernameOrEmail() == null || dto.getPassword() == null){
                log.error("Incomplete login data provided for: {}", dto.getUsernameOrEmail());

                applicationLogRepository.save(new ApplicationLog(
                        "ERROR",
                        "Incomplete login data provided for: " + dto.getUsernameOrEmail(),
                        rqid,
                        LocalDateTime.now(),
                        loggerName
                ));

                throw new IllegalArgumentException("Login information is incorrect");
            }

            // Проверка существует ли такой пользователь
            User user = userRepository.findByUsernameOrEmail(dto.getUsernameOrEmail(), dto.getUsernameOrEmail())
                    .orElseThrow(() -> {
                        log.warn("Login attempt for non-existent user: {}", dto.getUsernameOrEmail());

                        applicationLogRepository.save(new ApplicationLog(
                                "WARN",
                                "Login attempt for non-existent user: " + dto.getUsernameOrEmail(),
                                rqid,
                                LocalDateTime.now(),
                                loggerName
                        ));

                        return new BadCredentialsException("Invalid username or password");
                    });

            log.debug("Verifying password for user {}", user.getUsername());
            applicationLogRepository.save(new ApplicationLog(
                    "DEBUG",
                    "Verifying password for user: " + user.getUsername(),
                    rqid,
                    LocalDateTime.now(),
                    loggerName
            ));
            // Проверка зашифрованных паролей
            if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                log.error("Invalid password attempt for user {}", user.getUsername());

                applicationLogRepository.save(new ApplicationLog(
                        "ERROR",
                        "Invalid password attempt for user: " + user.getUsername(),
                        rqid,
                        LocalDateTime.now(),
                        loggerName
                ));

                throw new BadCredentialsException("Invalid username or password");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("user", UserMapper.usertoUserResponseDto(user));

            log.info("User {} successfully logged in", user.getUsername());

            applicationLogRepository.save(new ApplicationLog(
                    "INFO",
                    "User " + user.getUsername() + " logged in successfully",
                    rqid,
                    LocalDateTime.now(),
                    loggerName
            ));

            return response;
    }
}
