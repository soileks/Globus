package com.bank.userservice.service;

import com.bank.userservice.dto.RecaptchaResponseDto;
import com.bank.userservice.dto.RegistrationDto;
import com.bank.userservice.exception.InvalidCaptchaException;
import com.bank.userservice.repository.log.ApplicationLogRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.bank.userservice.model.log.enums.LogLevel.*;

/**
 * Сервис для проверки различных типов CAPTCHA.
 *
 * <p>Поддерживаемые типы CAPTCHA:
 * <ul>
 *   <li><b>reCAPTCHA</b> - проверка через Google API</li>
 *   <li><b>Математическая CAPTCHA</b> - решение простых арифметических задач</li>
 * </ul>
 *
 * <p>Сервис выполняет:
 * <ul>
 *   <li>Валидацию входных данных</li>
 *   <li>Логирование всех этапов проверки</li>
 *   <li>Интеграцию с внешними сервисами (Google reCAPTCHA)</li>
 *   <li>Генерацию ошибок при неудачной проверке</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Data
public class CaptchaService {
    // Секретный ключ для reCAPTCHA
    @Value("${recaptcha.secret-key}")
    private String secretKey;
    // Клиент для HTTP-запросов к сервису reCAPTCHA
    private RestTemplate restTemplate = new RestTemplate();

    private final ApplicationLogRepository applicationLogRepository;
    // URL API для проверки reCAPTCHA
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    // Поддерживаемые математические операции
    private static final List<String> SUPPORTED_MATH_OPS = List.of("+", "-", "*");
    // Имя логгера (используется для записи в логи)
    private final String loggerName = this.getClass().getName();
    private final ApplicationLogService applicationLogService;

    /**
     * Проверяет CAPTCHA в зависимости от указанного типа верификации.
     *
     * <p>В зависимости от verificationType вызывает соответствующую проверку:
     * <ul>
     *   <li>"recaptcha" - проверка через Google reCAPTCHA API</li>
     *   <li>"math" - проверка математической CAPTCHA</li>
     * </ul>
     *
     * @param registrationDto DTO регистрации содержащее:
     *           <ul>
     *             <li>verificationType - тип CAPTCHA ("recaptcha" или "math")</li>
     *             <li>recaptchaToken - токен для reCAPTCHA (если выбран этот тип)</li>
     *             <li>mathToken - математическая задача (если выбран этот тип)</li>
     *             <li>rqid - идентификатор запроса</li>
     *           </ul>
     * @throws InvalidCaptchaException если:
     *           <ul>
     *             <li>Указан неподдерживаемый тип верификации</li>
     *             <li>Проверка CAPTCHA не пройдена</li>
     *           </ul>
     * @throws IllegalArgumentException если не указан обязательный параметр
     *
     * @see RegistrationDto
     * @see InvalidCaptchaException
     */
    public void verifyCaptcha(RegistrationDto registrationDto) {

        String rqid = registrationDto.getRqid();
        applicationLogService.log(INFO,
                "Starting captcha verification for type: " + registrationDto.getVerificationType(),
                rqid,
                loggerName);

        boolean verify = switch (registrationDto.getVerificationType()) {
            case "recaptcha" -> verifyRecaptcha(registrationDto.getRecaptchaToken(), rqid);
            case "math" -> verifyMathCaptcha(registrationDto.getMathToken(), rqid);
            default -> {
                applicationLogService.log(WARN,
                        "Unsupported captcha type: " + registrationDto.getVerificationType(),
                        rqid,
                        loggerName);
                throw new InvalidCaptchaException("Unsupported verification type");

            }
        };

        if (!verify) {
            applicationLogService.log(ERROR,
                    "Captcha verification failed for type: " + registrationDto.getVerificationType(),
                    rqid,
                    loggerName);

            throw new InvalidCaptchaException("Incorrect captcha");
        }
        applicationLogService.log(INFO,
                "Captcha verification successful for type: " + registrationDto.getVerificationType(),
                rqid,
                loggerName);

    }

    /**
     * Проверяет токен reCAPTCHA через Google API.
     *
     * <p>Процесс проверки:
     * <ol>
     *   <li>Отправка POST-запроса к Google reCAPTCHA API</li>
     *   <li>Проверка ответа на успешность</li>
     *   <li>Логирование результата</li>
     * </ol>
     *
     * @param token токен reCAPTCHA полученный от клиента
     * @param rqid идентификатор запроса для логирования
     * @return true если проверка пройдена успешно, false в противном случае
     *
     * @see RecaptchaResponseDto
     */
    public boolean verifyRecaptcha(String token, String rqid) {
//        applicationLogService.log(DEBUG,
//                "Verifying reCAPTCHA token",
//                rqid,
//                loggerName);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", secretKey);
        params.add("response", token); // Токен от клиента
        // Отправка запроса к API reCAPTCHA
        RecaptchaResponseDto response = restTemplate.postForObject(
                VERIFY_URL,
                params,
                RecaptchaResponseDto.class
        );

        boolean success = response != null && response.isSuccess();

        if (!success) {
            applicationLogService.log(ERROR,
                    "reCAPTCHA verification failed",
                    rqid,
                    loggerName);
//            log.error("reCAPTCHA verification failed. Response: {}, ErrorCodes: {}",
//                    response, response != null ? response.getErrorCodes() : "null");
        }

        return success;
    }

    /**
     * Проверяет решение математической CAPTCHA.
     *
     * <p>Формат математической задачи:
     * <pre>"число1 операция число2 = ответ"</pre>
     *
     * <p>Поддерживаемые операции:
     * <ul>
     *   <li>Сложение (+)</li>
     *   <li>Вычитание (-)</li>
     *   <li>Умножение (*)</li>
     * </ul>
     *
     * @param mathToken строка с математической задачей в формате "a + b = c"
     * @param rqid идентификатор запроса для логирования
     * @return true если ответ верный, false в противном случае
     * @throws InvalidCaptchaException если неверный формат строки с задачей
     * @throws NumberFormatException если числа в задаче не являются целыми числами
     * @throws IllegalArgumentException если mathToken некорректный
     */
    public boolean verifyMathCaptcha(String mathToken, String rqid) {
//        applicationLogService.log( DEBUG,
//                "Verifying math captcha. Problem: " + problem + ", Answer: " + answer,
//                rqid,
//                loggerName);

        if (mathToken == null) {
            applicationLogService.log(ERROR,
                    "Math token must be provided",
                    rqid,
                    loggerName);
            throw new IllegalArgumentException("Math token must be provided");
        }
        String[] parts = mathToken.trim().split("\\s+");

        if (parts.length != 5) {
            applicationLogService.log(ERROR,
                    "Invalid math token format",
                    rqid,
                    loggerName);
            throw new InvalidCaptchaException("Invalid math token format");
        }

        int userAnswer = Integer.parseInt(parts[4]);

        int num1 = Integer.parseInt(parts[0]);
        int num2 = Integer.parseInt(parts[2]);
        String op = parts[1];


        if (!SUPPORTED_MATH_OPS.contains(op)) {
            applicationLogService.log(ERROR,
                    "Unsupported math operator: " + op,
                    rqid,
                    loggerName);

            throw new IllegalArgumentException("Unsupported math operator. Allowed: +, -, *");
        }
        int correctAnswer = switch (op) {
            case "+" -> num1 + num2;
            case "-" -> num1 - num2;
            case "*" -> num1 * num2;
            default -> throw new IllegalArgumentException("Unsupported math operator");
        };

        boolean result = userAnswer == correctAnswer;

        if (!result) {
            applicationLogService.log(ERROR,
                    "Math captcha failed. Expected: " + correctAnswer + ", got: " + userAnswer,
                    rqid,
                    loggerName);
        }

        return result;

    }

}