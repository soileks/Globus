package com.bank.userservice.service;

import com.bank.userservice.dto.EmailVerificationDto;
import com.bank.userservice.dto.LoginDto;
import com.bank.userservice.dto.RegistrationDto;
import com.bank.userservice.dto.auth.AuthResponseDto;
import com.bank.userservice.exception.AccountExpiredException;
import com.bank.userservice.exception.EmailNotVerifiedException;
import com.bank.userservice.model.User;
import com.bank.userservice.mapper.UserMapper;
import com.bank.userservice.repository.log.ApplicationLogRepository;
import com.bank.userservice.repository.UserRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.mail.MessagingException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.bank.userservice.model.log.enums.LogLevel.*;


/**
 * Сервис для обработки аутентификации и регистрации пользователей.
 * Обеспечивает регистрацию, вход, подтверждение email и связанные функции.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Data
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final UserRepository userRepository;
    private final ApplicationLogRepository applicationLogRepository;
    private final ApplicationLogService applicationLogService;
    private final String loggerName = this.getClass().getName();
    private final IntegrationLogService integrationLogService;
    private final EmailService emailService;
    /**
     * Срок действия токена */
    @Value("${email.verification.token.expiration.minutes}")
    private int tokenExpirationMinutes;

    /**
     * Регистрирует нового пользователя в системе.
     *
     * <p>Процесс регистрации включает:
     * <ol>
     *   <li>Проверку капчи</li>
     *   <li>Валидацию обязательных полей</li>
     *   <li>Проверку на существование пользователя с таким же email/username</li>
     *   <li>Создание новой учетной записи</li>
     *   <li>Отправку email для подтверждения регистрации</li>
     * </ol>
     *
     * @param registrationDto DTO содержащий данные для регистрации:
     *                       <ul>
     *                         <li>username (2-20 символов)</li>
     *                         <li>email (валидный email адрес)</li>
     *                         <li>password (5-20 символов)</li>
     *                         <li>verificationType (тип капчи)</li>
     *                         <li>rqid (идентификатор запроса)</li>
     *                         <li>mathToken или recaptchaToken</li>
     *                       </ul>
     * @return AuthResponseDto с результатом регистрации, содержащий:
     *         <ul>
     *           <li>message - статус операции</li>
     *           <li>user - данные зарегистрированного пользователя</li>
     *         </ul>
     * @throws IllegalArgumentException если не заполнены обязательные поля
     * @throws BadCredentialsException если:
     *         <ul>
     *           <li>Пользователь с таким email/username уже существует</li>
     *           <li>Аккаунт не подтвержден, но срок токена еще действует</li>
     *         </ul>
     * @throws JsonProcessingException при ошибках обработки JSON
     * @throws MessagingException при ошибках отправки email
     *
     * @see RegistrationDto
     * @see AuthResponseDto
     */
    @Transactional
    public AuthResponseDto register(RegistrationDto registrationDto) throws JsonProcessingException, MessagingException {
        String rqid = registrationDto.getRqid();

        applicationLogService.log(INFO,
                "Starting registration process for user: " + registrationDto.getUsername(),
                rqid,
                loggerName);

        captchaService.verifyCaptcha(registrationDto);
        //User user = registerUser(dto);
        String username = registrationDto.getUsername();
        String email = registrationDto.getEmail();

        // Проверка обязательных полей
        if (username == null || registrationDto.getPassword() == null || email == null) {
            applicationLogService.log(ERROR, "Incomplete registration data", rqid, loggerName);
            throw new IllegalArgumentException("Registration data is incorrect");
        }

        // Проверка существующих пользователей
        Optional<User> existingUserByUsername = userRepository.findByUsername(username);
        Optional<User> existingUserByEmail = userRepository.findByEmail(email);

        // Обработка случая, когда пользователь уже существует
        if (existingUserByUsername.isPresent() || existingUserByEmail.isPresent()) {
            handleExistingUser(existingUserByUsername.orElseGet(() -> existingUserByEmail.get()),
                    registrationDto);
        }
        User user = createNewUser(registrationDto);

        applicationLogService.log(INFO,
                "User " + registrationDto.getUsername() + " registered successfully ",
                rqid,
                loggerName);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registration successful");
        response.put("user", UserMapper.usertoUserResponseDto(user));


        return integrationLogService.logInteraction(response);
    }

    /**
     * Обрабатывает ситуацию с уже существующим пользователем при регистрации.
     *
     * <p>Проверяет статус существующего пользователя:
     * <ul>
     *   <li>Если email уже подтвержден - выбрасывает исключение</li>
     *   <li>Если email не подтвержден и срок токена истек - удаляет старую запись</li>
     *   <li>Если email не подтвержден, но срок токена действует - выбрасывает исключение</li>
     * </ul>
     *
     * @param existingUser существующий пользователь, найденный по email или username
     * @param registrationDto DTO с данными регистрации
     * @throws BadCredentialsException с соответствующим сообщением об ошибке:
     *         <ul>
     *           <li>"Username already exists"</li>
     *           <li>"Email already registered"</li>
     *           <li>"Account not verified. Check your email or try again later"</li>
     *         </ul>
     *
     * @see User
     * @see RegistrationDto
     */
    private void handleExistingUser(User existingUser, RegistrationDto registrationDto) {
        String rqid = registrationDto.getRqid();
        // Если аккаунт подтверждён - ошибка
        if (existingUser.isEmailVerified()) {
            String message = existingUser.getUsername().equals(registrationDto.getUsername())
                    ? "Username already exists"
                    : "Email already registered";

            applicationLogService.log(WARN,
                    "Registration attempt with existing verified credentials: " + message,
                    rqid,
                    loggerName);
            throw new BadCredentialsException(message);
        }

        // Если аккаунт не подтверждён и срок истёк - удаляем старую запись
        if (existingUser.getCreatedAt().isBefore(LocalDateTime.now().minusDays(1))) {
            applicationLogService.log(INFO,
                    "Deleting expired unverified user: " + existingUser.getEmail(),
                    rqid,
                    loggerName);
            userRepository.delete(existingUser);
        }
        // Если аккаунт не подтверждён, но срок ещё действует - ошибка
        else {
            applicationLogService.log(WARN,
                    "Registration attempt with existing unverified credentials",
                    rqid,
                    loggerName);
            throw new BadCredentialsException("Account not verified. Check your email or try again later");
        }
    }

    /**
     * Создает и сохраняет нового пользователя в базе данных.
     *
     * <p>Выполняет:
     * <ol>
     *   <li>Создание новой учетной записи</li>
     *   <li>Хеширование пароля</li>
     *   <li>Генерацию токена верификации email</li>
     *   <li>Установку срока действия токена (24 часа по умолчанию)</li>
     *   <li>Отправку письма с подтверждением</li>
     * </ol>
     *
     * @param registrationDto DTO с данными для регистрации
     * @return сохраненный объект User со следующими полями:
     *         <ul>
     *           <li>emailVerified = false</li>
     *           <li>emailVerificationToken - сгенерированный токен</li>
     *           <li>emailVerificationTokenExpiresAt - срок действия токена</li>
     *         </ul>
     * @throws MessagingException при ошибках отправки email подтверждения
     *
     * @see RegistrationDto
     * @see User
     */
    private User createNewUser(RegistrationDto registrationDto) throws MessagingException {
        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setEmailVerified(false);

        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));

        String token = UUID.randomUUID() + "-" + System.currentTimeMillis();
        user.setEmailVerificationToken(token);
        user.setEmailVerificationTokenExpiresAt(
                LocalDateTime.now().plusMinutes(tokenExpirationMinutes)
        );

        User savedUser = userRepository.save(user);
        emailService.sendVerificationEmail(registrationDto.getEmail(), token, registrationDto.getRqid());

        return savedUser;
    }

    /**
     * Проверяет и подтверждает email пользователя на основе переданного токена верификации.
     *
     * <p>Процесс верификации включает следующие проверки:
     * <ol>
     *   <li>Существование пользователя с указанным email</li>
     *   <li>Совпадение переданного токена с токеном в базе данных</li>
     *   <li>Срок действия токена верификации</li>
     *   <li>Отсутствие предыдущего подтверждения email</li>
     * </ol>
     *
     * @param emailVerificationDto DTO содержащий:
     *                            <ul>
     *                              <li>email - адрес для подтверждения</li>
     *                              <li>token - токен верификации</li>
     *                              <li>rqid - идентификатор запроса</li>
     *                            </ul>
     * @return AuthResponseDto с данными подтвержденного пользователя и статусом операции
     * @throws BadCredentialsException в случаях:
     *                               <ul>
     *                                 <li>Пользователь с указанным email не найден</li>
     *                                 <li>Неверный токен верификации</li>
     *                                 <li>Истек срок действия токена</li>
     *                                 <li>Email уже подтвержден</li>
     *                               </ul>
     * @throws JsonProcessingException при ошибках сериализации/десериализации JSON
     *
     * @see EmailVerificationDto
     * @see AuthResponseDto
     */
    @Transactional
    public AuthResponseDto verifyEmail(EmailVerificationDto emailVerificationDto) throws JsonProcessingException {
        String rqid = emailVerificationDto.getRqid();

        applicationLogService.log(INFO,
                "Starting email verification for: " + emailVerificationDto.getEmail(),
                rqid,
                loggerName);

        // Находим пользователя
        User user = userRepository.findByEmail(emailVerificationDto.getEmail())
                .orElseThrow(() -> {
                    applicationLogService.log(ERROR,
                            "User not found for email: " + emailVerificationDto.getEmail(),
                            rqid,
                            loggerName);
                    return new BadCredentialsException("User not found");
                });

        // Проверяем токен
        if (!emailVerificationDto.getToken().equals(user.getEmailVerificationToken())) {
            applicationLogService.log(ERROR,
                    "Invalid verification token for email: " + emailVerificationDto.getEmail(),
                    rqid,
                    loggerName);
            throw new BadCredentialsException("Invalid verification token");
        }

        // Проверяем срок действия токена
        if (user.getEmailVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            applicationLogService.log(ERROR,
                    "Expired verification token for email: " + emailVerificationDto.getEmail(),
                    rqid,
                    loggerName);
            throw new BadCredentialsException("Verification token has expired");
        }

        // Проверяем, не подтвержден ли уже email
        if (user.isEmailVerified()) {
            applicationLogService.log(WARN,
                    "Email already verified: " + emailVerificationDto.getEmail(),
                    rqid,
                    loggerName);
            throw new BadCredentialsException("Email already verified");
        }

        // Подтверждаем email
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiresAt(null);
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Email successfully verified");
        response.put("user", UserMapper.usertoUserResponseDto(user));

        applicationLogService.log(INFO,
                "Email verified successfully for: " + emailVerificationDto.getEmail(),
                rqid,
                loggerName);

        return integrationLogService.logInteraction(response);
    }

    /**
     * Выполняет аутентификацию пользователя в системе.
     *
     * <p>Процесс аутентификации включает:
     * <ol>
     *   <li>Проверку наличия обязательных полей (email и пароль)</li>
     *   <li>Поиск пользователя по email</li>
     *   <li>Проверку статуса учетной записи (подтверждение email)</li>
     *   <li>Сравнение хешей паролей</li>
     *   <li>Отправку уведомления о входе на email</li>
     *   <li>Логирование успешной аутентификации</li>
     * </ol>
     *
     * @param loginDto DTO содержащий данные для входа:
     *                <ul>
     *                  <li>email (обязательное поле)</li>
     *                  <li>password (обязательное поле)</li>
     *                  <li>rqid (идентификатор запроса)</li>
     *                </ul>
     * @return AuthResponseDto с результатом аутентификации, содержащий:
     *         <ul>
     *           <li>message - статус операции ("Login successful")</li>
     *           <li>user - данные аутентифицированного пользователя</li>
     *         </ul>
     * @throws IllegalArgumentException если не заполнены обязательные поля
     * @throws BadCredentialsException если:
     *         <ul>
     *           <li>Пользователь с указанным email не найден</li>
     *           <li>Неверный пароль</li>
     *         </ul>
     * @throws AccountExpiredException если срок регистрации истек (email не подтвержден вовремя)
     * @throws EmailNotVerifiedException если email не подтвержден (но срок еще действует)
     * @throws JsonProcessingException при ошибках сериализации ответа
     *
     * @see LoginDto
     * @see AuthResponseDto
     */
    public AuthResponseDto login(LoginDto loginDto) throws JsonProcessingException {
        String rqid = loginDto.getRqid();
        applicationLogService.log(INFO,
                "Login attempt processing for: " + loginDto.getEmail(),
                rqid,
                loggerName);

        if (loginDto.getEmail() == null || loginDto.getPassword() == null) {

            applicationLogService.log(ERROR,
                    "Incomplete login data provided for: " + loginDto.getEmail(),
                    rqid,
                    loggerName);
            throw new IllegalArgumentException("Login information is incorrect");
        }

        // Проверка существует ли такой пользователь
        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> {
                    applicationLogService.log(ERROR,
                            "Login attempt for non-existent user: " + loginDto.getEmail(),
                            rqid,
                            loggerName);

                    return new BadCredentialsException("Invalid username or password");
                });

        checkAccountStatus(user, rqid);

//        applicationLogService.log(DEBUG,
//                "Verifying password for user: " + user.getUsername(),
//                rqid,
//                loggerName);

        // Проверка зашифрованных паролей
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            applicationLogService.log(ERROR,
                    "Invalid password attempt for user: " + user.getUsername(),
                    rqid,
                    loggerName);

            throw new BadCredentialsException("Invalid username or password");
        }

        emailService.sendLoginNotification(user.getEmail(), user.getUsername(), rqid);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("user", UserMapper.usertoUserResponseDto(user));
        applicationLogService.log(
                INFO,
                "User " + user.getUsername() + " logged in successfully",
                rqid,
                loggerName
        );

        return integrationLogService.logInteraction(response);
    }
    /**
     * Проверяет статус учетной записи пользователя перед аутентификацией.
     *
     * <p>Выполняет следующие проверки:
     * <ol>
     *   <li>Подтвержден ли email пользователя</li>
     *   <li>Актуален ли срок действия токена верификации (если email не подтвержден)</li>
     * </ol>
     *
     * @param user пользователь, для которого происходит проверка
     * @param rqid идентификатор запроса (для логирования в случае неудачи)
     * @throws AccountExpiredException если истек срок действия токена
     * @throws EmailNotVerifiedException если срок действия токена не истек и email не подтвержден
     */
    private void checkAccountStatus(User user, String rqid) {
        if (!user.isEmailVerified()) {
            if (user.getEmailVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
                userRepository.delete(user);
                applicationLogService.log(ERROR,
                        "Registration expired. Please register again",
                        rqid,
                        loggerName);
                throw new AccountExpiredException("Registration expired. Please register again");
            }
            applicationLogService.log(ERROR,
                    "Email not verified. Check your inbox",
                    rqid,
                    loggerName);
            throw new EmailNotVerifiedException("Email not verified. Check your inbox");
        }
    }
}
