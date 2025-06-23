package com.bank.userservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import static com.bank.userservice.model.log.enums.LogLevel.ERROR;
import static com.bank.userservice.model.log.enums.LogLevel.INFO;
/**
 * Сервис для отправки электронных писем.
 *
 * <p>Основные функции:
 * <ul>
 *   <li>Отправка уведомлений о входе в систему</li>
 *   <li>Отправка писем для подтверждения email</li>
 *   <li>Логирование результатов отправки</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Data
public class EmailService {
    private final JavaMailSender mailSender;
    private final ApplicationLogService applicationLogService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.verification-url}")
    private String verificationBaseUrl;

    /**
     * Отправляет уведомление о входе в систему.
     *
     * <p>Содержит информацию:
     * <ul>
     *   <li>Имя пользователя</li>
     *   <li>Дату и время входа</li>
     *   <li>Рекомендации при несанкционированном доступе</li>
     * </ul>
     *
     * @param toEmail email адрес получателя
     * @param username имя пользователя для персонализации письма
     * @param rqid идентификатор запроса для логирования
     */
     void sendLoginNotification(String toEmail, String username, String rqid) {
        String subject = "Уведомление о входе в систему";
        String text = String.format("""
            Уважаемый %s,
            
            Вы успешно вошли в систему.
            
            Дата и время входа: %s
            
            Если это были не вы, пожалуйста, немедленно свяжитесь со службой поддержки.
            
            С уважением,
            Команда поддержки
            """, username, LocalDateTime.now());

        sendEmail(createSimpleMessage(toEmail, subject, text), rqid);
    }

    /**
     * Отправляет письмо для подтверждения email.
     *
     * <p>Письмо содержит:
     * <ul>
     *   <li>HTML-верстку</li>
     *   <li>Кнопку с ссылкой для подтверждения</li>
     *   <li>Сгенерированный верификационный URL</li>
     * </ul>
     *
     * @param toEmail email адрес для подтверждения
     * @param token верификационный токен
     * @param rqid идентификатор запроса для логирования
     * @throws MessagingException при ошибках создания/отправки MIME сообщения
     */
     void sendVerificationEmail(String toEmail, String token, String rqid) throws MessagingException {
        String subject = "Подтверждение email адреса";
        String verificationUrl = createVerificationUrl(toEmail, token, rqid);
        String htmlContent = createVerificationEmailContent(verificationUrl);

        sendHtmlEmail(createMimeMessage(toEmail, subject, htmlContent), rqid);
    }

    /**
     * Создает простое текстовое email сообщение.
     *
     * @param to email получателя
     * @param subject тема письма
     * @param text текст письма
     * @return готовое к отправке сообщение
     */
     SimpleMailMessage createSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        return message;
    }
    /**
     * Создает MIME сообщение с HTML содержимым.
     *
     * @param to email получателя
     * @param subject тема письма
     * @param htmlContent HTML содержимое письма
     * @return готовое MIME сообщение
     * @throws MessagingException при ошибках создания MIME сообщения
     */
     MimeMessage createMimeMessage(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        return message;
    }
    /**
     * Генерирует URL для подтверждения email.
     *
     * <p>Формат URL:
     * <pre>{baseUrl}?email={encodedEmail}&token={encodedToken}&rqid={encodedRqid}</pre>
     *
     * @param email email для подтверждения
     * @param token верификационный токен
     * @param rqid идентификатор запроса
     * @return URL для подтверждения в encoded виде
     */
    public String createVerificationUrl(String email, String token, String rqid) {
        return verificationBaseUrl +
                "?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8) +
                "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8) +
                "&rqid=" + URLEncoder.encode(rqid, StandardCharsets.UTF_8);
    }

    /**
     * Генерирует HTML содержимое для письма подтверждения.
     *
     * @param verificationUrl URL для подтверждения
     * @return HTML содержимое письма
     */
     String createVerificationEmailContent(String verificationUrl) {
        return String.format("""
        <html>
        <body style="font-family: Arial, sans-serif;">
            <h2>Подтверждение регистрации</h2>
            <p>Для завершения регистрации нажмите кнопку:</p>
            
            <a href="%s" 
               style="display: inline-block; padding: 10px 20px; 
                      background-color: #4CAF50; color: white; 
                      text-decoration: none; border-radius: 5px;">
                Подтвердить Email
            </a>
        </body>
        </html>
        """, verificationUrl);
    }

    /**
     * Отправляет HTML email сообщение.
     *
     * @param message готовое MIME сообщение
     * @param rqid идентификатор запроса для логирования
     * @throws MessagingException при ошибках отправки
     */
    public void sendHtmlEmail(MimeMessage message, String rqid) throws MessagingException {
        try {
            mailSender.send(message);
            logSuccess("HTML email", message.getAllRecipients()[0].toString(), rqid);
        } catch (Exception e) {
            handleEmailError("HTML email", message.getAllRecipients()[0].toString(), e, rqid);
        }
    }
    /**
     * Отправляет простое текстовое email сообщение.
     *
     * @param message готовое сообщение
     * @param rqid идентификатор запроса для логирования
     */
    public void sendEmail(SimpleMailMessage message, String rqid) {
        try {
            mailSender.send(message);
            logSuccess("email", message.getTo()[0], rqid);
        } catch (Exception e) {
            handleEmailError("email", message.getTo()[0], e, rqid);
        }
    }

    /**
     * Логирует успешную отправку письма.
     *
     * @param emailType тип письма ("email" или "HTML email")
     * @to email адрес получателя
     * @rqid идентификатор запроса
     */
    public void logSuccess(String emailType, String to, String rqid) {
        applicationLogService.log(INFO,
                String.format("%s sent successfully to: %s", emailType, to),
                rqid,
                this.getClass().getName());
    }
    /**
     * Логирует ошибку при отправке письма.
     *
     * @param emailType тип письма ("email" или "HTML email")
     * @to email адрес получателя
     * @e исключение, вызвавшее ошибку
     * @rqid идентификатор запроса
     * @throws RuntimeException обернутое исключение
     */
    public void handleEmailError(String emailType, String to, Exception e, String rqid) {
        applicationLogService.log(ERROR,
                String.format("Failed to send %s to: %s. Error: %s", emailType, to, e.getMessage()),
                rqid,
                this.getClass().getName());
        throw new RuntimeException("Failed to send email", e);
    }
}