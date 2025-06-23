package com.bank.userservice.service;

import jakarta.mail.Address;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static com.bank.userservice.model.log.enums.LogLevel.ERROR;
import static com.bank.userservice.model.log.enums.LogLevel.INFO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.Captor;

import jakarta.mail.MessagingException;

import java.time.LocalDateTime;

/**
 * Тестовый класс для {@link EmailService}.
 *
 * <p>Проверяет:
 * <ul>
 *   <li>Отправку различных типов email-сообщений</li>
 *   <li>Формирование MIME-сообщений</li>
 *   <li>Обработку ошибок при отправке</li>
 *   <li>Логирование операций</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ApplicationLogService applicationLogService;

    @Mock
    private MimeMessage mimeMessage;
    /** Захватчик для простых email-сообщений */
    @Captor
    private ArgumentCaptor<SimpleMailMessage> simpleMessageCaptor;
    /** Захватчик для MIME email-сообщений */
    @Captor
    private ArgumentCaptor<MimeMessage> mimeMessageCaptor;

    @InjectMocks
    private EmailService emailService;

    /** Тестовый email */
    private final String testEmail = "test@example.com";
    /** Тестовое имя пользователя */
    private final String testUsername = "testUser";
    /** Тестовый идентификатор запроса */
    private final String testRqid = "test-rqid";
    /** Тестовый токен */
    private final String testToken = "test-token-123";

    /**
     * Настройка тестового окружения перед каждым тестом.
     * Устанавливает базовые параметры email-сервиса.
     */
    @BeforeEach
    void setUp() {
        emailService.setFromEmail("noreply@example.com");
        emailService.setVerificationBaseUrl("http://example.com/verify");
    }

    /**
     * Проверяет отправку уведомления о входе.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Правильность заполнения полей сообщения</li>
     *   <li>Формат текста сообщения</li>
     *   <li>Логирование успешной отправки</li>
     * </ul>
     */
    @Test
    void sendLoginNotification_ShouldCreateAndSendCorrectMessage() {

        emailService.sendLoginNotification(testEmail, testUsername, testRqid);

        verify(mailSender).send(simpleMessageCaptor.capture());
        SimpleMailMessage sentMessage = simpleMessageCaptor.getValue();

        assertEquals("noreply@example.com", sentMessage.getFrom());
        assertEquals(testEmail, sentMessage.getTo()[0]);
        assertTrue(sentMessage.getSubject().contains("Уведомление о входе в систему"));
        assertTrue(sentMessage.getText().contains(testUsername));
        assertTrue(sentMessage.getText().contains(LocalDateTime.now().getYear() + ""));

        verifyLogSuccess("email", testEmail);
    }

    /**
     * Проверяет отправку HTML-письма для подтверждения email.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Создание MIME-сообщения</li>
     *   <li>Настройку получателей</li>
     *   <li>Факт отправки сообщения</li>
     * </ul>
     */
    @Test
    void sendVerificationEmail_ShouldCreateAndSendCorrectHtmlMessage() throws MessagingException {

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Address[] recipients = new jakarta.mail.Address[]{
                new jakarta.mail.internet.InternetAddress(testEmail)
        };
        when(mimeMessage.getAllRecipients()).thenReturn(recipients);

        emailService.sendVerificationEmail(testEmail, testToken, testRqid);


        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessageCaptor.capture());
        assertEquals(mimeMessage, mimeMessageCaptor.getValue());

        verifyLogSuccess("HTML email", testEmail);
    }

    /**
     * Проверяет создание простого текстового сообщения.
     *
     * <p>Проверяет корректность заполнения всех полей:
     * <ul>
     *   <li>Отправитель</li>
     *   <li>Получатель</li>
     *   <li>Тема</li>
     *   <li>Текст</li>
     * </ul>
     */
    @Test
    void createSimpleMessage_ShouldSetAllRequiredFields() {

        String to = "to@example.com";
        String subject = "Test Subject";
        String text = "Test Text";

        SimpleMailMessage message = emailService.createSimpleMessage(to, subject, text);

        assertEquals(emailService.getFromEmail(), message.getFrom());
        assertEquals(to, message.getTo()[0]);
        assertEquals(subject, message.getSubject());
        assertEquals(text, message.getText());
    }

    /**
     * Проверяет корректность создания MIME сообщения.
     *
     * <p>Тест проверяет:
     * <ul>
     *   <li>Создание сообщения через mailSender</li>
     *   <li>Установку темы сообщения</li>
     *   <li>Возврат корректного MimeMessage объекта</li>
     * </ul>
     */
    @Test
    void createMimeMessage_ShouldConfigureHelperCorrectly() throws MessagingException {

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        String to = "to@example.com";
        String subject = "Test Subject";
        String html = "<html>Test</html>";

        MimeMessage result = emailService.createMimeMessage(to, subject, html);

        verify(mailSender).createMimeMessage();
        verify(mimeMessage).setSubject(subject, "UTF-8");
        assertEquals(mimeMessage, result);
    }
    /**
     * Проверяет генерацию URL для подтверждения email.
     *
     * <p>Тест проверяет:
     * <ul>
     *   <li>Кодирование всех параметров (email, token, rqid)</li>
     *   <li>Формат итогового URL</li>
     *   <li>Корректность базового URL</li>
     *   <li>Наличие всех обязательных параметров</li>
     * </ul>
     */
    @Test
    void createVerificationUrl_ShouldEncodeAllParameters() {

        String email = "user@test.com";
        String token = "token123";
        String rqid = "req123";

        String url = emailService.createVerificationUrl(email, token, rqid);


        assertTrue(url.contains("email=user%40test.com"));
        assertTrue(url.contains("token=token123"));
        assertTrue(url.contains("rqid=req123"));
        assertTrue(url.startsWith("http://example.com/verify?"));
    }
    /**
     * Проверяет генерацию HTML содержимого для письма подтверждения.
     *
     * <p>Тест проверяет:
     * <ul>
     *   <li>Наличие верификационной ссылки в контенте</li>
     *   <li>Присутствие ключевых элементов (кнопка подтверждения)</li>
     *   <li>Корректность CSS стилей</li>
     * </ul>
     */
    @Test
    void createVerificationEmailContent_ShouldContainVerificationLink() {

        String verificationUrl = "http://test.com/verify?token=123";

        String content = emailService.createVerificationEmailContent(verificationUrl);

        assertTrue(content.contains(verificationUrl));
        assertTrue(content.contains("Подтвердить Email"));
        assertTrue(content.contains("font-family: Arial"));
    }
    /**
     * Проверяет обработку ошибок при отправке простого email.
     *
     * <p>Тест проверяет:
     * <ul>
     *   <li>Проброс исключения при ошибке отправки</li>
     *   <li>Логирование ошибки с правильными параметрами</li>
     *   <li>Корректность уровня логирования (ERROR)</li>
     *   <li>Наличие email получателя в логе</li>
     * </ul>
     */
    @Test
    void sendEmail_ShouldLogErrorWhenSendingFails() {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(testEmail);
        RuntimeException exception = new RuntimeException("Mail error");
        doThrow(exception).when(mailSender).send(message);

        assertThrows(RuntimeException.class, () -> {
            emailService.sendEmail(message, testRqid);
        });

        verify(applicationLogService).log(
                eq(ERROR),
                contains("Failed to send email to: " + testEmail),
                eq(testRqid),
                anyString()
        );
    }
    /**
     * Проверяет обработку ошибок при отправке HTML email.
     *
     * <p>Тест проверяет:
     * <ul>
     *   <li>Проброс исключения при ошибке отправки</li>
     *   <li>Логирование ошибки для HTML письма</li>
     *   <li>Корректность заполнения получателей</li>
     *   <li>Соответствие формата лога</li>
     * </ul>
     */
    @Test
    void sendHtmlEmail_ShouldLogErrorWhenSendingFails() throws MessagingException {

        when(mimeMessage.getAllRecipients()).thenReturn(new jakarta.mail.Address[]{
                new jakarta.mail.internet.InternetAddress(testEmail)
        });
        RuntimeException exception = new RuntimeException("Mail error");
        doThrow(exception).when(mailSender).send(mimeMessage);


        assertThrows(RuntimeException.class, () -> {
            emailService.sendHtmlEmail(mimeMessage, testRqid);
        });

        verify(applicationLogService).log(
                eq(ERROR),
                contains("Failed to send HTML email to: " + testEmail),
                eq(testRqid),
                anyString()
        );
    }
    /**
     * Проверяет обработку ошибок email сервиса.
     *
     * <p>Тест проверяет:
     * <ul>
     *   <li>Проброс исходного исключения</li>
     *   <li>Логирование ошибки с указанием типа email</li>
     *   <li>Корректность передаваемых в лог параметров</li>
     *   <li>Использование правильного уровня логирования</li>
     * </ul>
     */
    @Test
    void handleEmailError_ShouldLogAndRethrowException() {

        String emailType = "Test email";
        Exception exception = new RuntimeException("Test error");


        assertThrows(RuntimeException.class, () -> {
            emailService.handleEmailError(emailType, testEmail, exception, testRqid);
        });

        verify(applicationLogService).log(
                eq(ERROR),
                contains("Failed to send Test email to: " + testEmail),
                eq(testRqid),
                anyString()
        );
    }
    /**
     * Проверяет логирование успешной отправки email.
     *
     * <p>Тест проверяет:
     * <ul>
     *   <li>Факт вызова сервиса логирования</li>
     *   <li>Корректность передаваемых параметров</li>
     *   <li>Уровень логирования (INFO)</li>
     *   <li>Формат сообщения в логе</li>
     *   <li>Наличие email получателя в логе</li>
     * </ul>
     */
    @Test
    void logSuccess_ShouldCallApplicationLogService() {

        String emailType = "Test email";

        emailService.logSuccess(emailType, testEmail, testRqid);

        verify(applicationLogService).log(
                eq(INFO),
                contains("Test email sent successfully to: " + testEmail),
                eq(testRqid),
                anyString()
        );
    }
    /**
     * Вспомогательный метод для проверки логирования успешной операции.
     *
     * @param emailType тип отправленного email
     * @param email адрес получателя
     */
    private void verifyLogSuccess(String emailType, String email) {
        verify(applicationLogService).log(
                eq(INFO),
                contains(emailType + " sent successfully to: " + email),
                eq(testRqid),
                anyString()
        );
    }
}
