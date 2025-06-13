package com.bank.userservice.service;

import com.bank.userservice.dto.RecaptchaResponseDto;
import com.bank.userservice.dto.RegistrationDto;
import com.bank.userservice.model.Log.ApplicationLog;
import com.bank.userservice.repository.Log.ApplicationLogRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Data
public class CaptchaService {
    @Value("${recaptcha.secret-key}")
    private String secretKey;

    private RestTemplate restTemplate = new RestTemplate();
    private final  ApplicationLogRepository applicationLogRepository;

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final List<String> SUPPORTED_MATH_OPS = List.of("+", "-", "*");
    private final String loggerName = this.getClass().getName();

    public void verifyCaptcha(RegistrationDto dto, Long rqid) {

        log.debug("Starting captcha verification for type: {}", dto.getVerificationType());
        applicationLogRepository.save(new ApplicationLog(
                "DEBUG",
                "Starting captcha verification for type: " + dto.getVerificationType(),
                rqid,
                LocalDateTime.now(),
                loggerName
        ));

        boolean verify;
        switch (dto.getVerificationType()) {
            case "recaptcha":
                verify = verifyRecaptcha(dto.getRecaptchaToken(), rqid);
                break;
            case "math":
                verify = verifyMathCaptcha(
                        dto.getMathProblem(),
                        dto.getMathAnswer(),
                        rqid
                );
                break;
            default:
                log.warn("Unsupported verification type: {}", dto.getVerificationType());
                applicationLogRepository.save(new ApplicationLog(
                        "WARN",
                        "Unsupported captcha type: " + dto.getVerificationType(),
                        rqid,
                        LocalDateTime.now(),
                        loggerName
                ));
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported verification type"
                );
        }

        if (!verify) {
            log.error("Captcha verification failed for type: {}", dto.getVerificationType());
            applicationLogRepository.save(new ApplicationLog(
                    "ERROR",
                    "Captcha verification failed for type: " + dto.getVerificationType(),
                    rqid,
                    LocalDateTime.now(),
                    loggerName
            ));
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Incorrect captcha"
            );
        }

        log.info("Captcha verification successful for type: {}", dto.getVerificationType());
        applicationLogRepository.save(new ApplicationLog(
                "INFO",
                "Captcha verification successful for type: " + dto.getVerificationType(),
                rqid,
                LocalDateTime.now(),
                loggerName
        ));

    }

    public boolean verifyRecaptcha(String token, Long rqid) {

        log.debug("Verifying reCAPTCHA token");
        applicationLogRepository.save(new ApplicationLog(
                "DEBUG",
                "Verifying reCAPTCHA token",
                rqid,
                LocalDateTime.now(),
                loggerName
        ));
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", secretKey);
        params.add("response", token);

        RecaptchaResponseDto response = restTemplate.postForObject(
                VERIFY_URL,
                params,
                RecaptchaResponseDto.class
        );

        boolean success = response != null && response.isSuccess();

        if (!success) {
            log.error("reCAPTCHA verification failed. Response: {}, ErrorCodes: {}",
                    response, response != null ? response.getErrorCodes() : "null");
            applicationLogRepository.save(new ApplicationLog(
                    "ERROR",
                    "reCAPTCHA verification failed",
                    rqid,
                    LocalDateTime.now(),
                    loggerName
            ));
        }

        return success;
    }

    public boolean verifyMathCaptcha(String problem, String answer, Long rqid) {

        log.debug("Verifying math captcha. Problem: {}, Answer: {}", problem, answer);
        applicationLogRepository.save(new ApplicationLog(
                "DEBUG",
                "Verifying math captcha. Problem: " + problem + ", Answer: " + answer,
                rqid,
                LocalDateTime.now(),
                loggerName
        ));
        if (problem == null || answer == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Math problem and answer must be provided"
            );
        }

        try {
            int userAnswer = Integer.parseInt(answer.trim());
            String[] parts = problem.trim().split("\\s+");

            if (parts.length != 3) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Math problem must be in format 'num1 op num2'"
                );
            }

            int num1 = Integer.parseInt(parts[0]);
            int num2 = Integer.parseInt(parts[2]);
            String op = parts[1];

            log.debug("Math captcha operands: {} {}, operation: {}", num1, num2, op);

            if (!SUPPORTED_MATH_OPS.contains(op)) {
                log.error("Unsupported math operator: {}", op);
                applicationLogRepository.save(new ApplicationLog(
                        "ERROR",
                        "Unsupported math operator: " + op,
                        rqid,
                        LocalDateTime.now(),
                        loggerName
                ));
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported math operator. Allowed: +, -, *"
                );
            }

            int correctAnswer = switch (op) {
                case "+" -> num1 + num2;
                case "-" -> num1 - num2;
                case "*" -> num1 * num2;
                default -> throw new IllegalStateException("Unsupported operator");
            };

            boolean result = userAnswer == correctAnswer;

            if (!result) {
                log.error("Math captcha failed. Expected: {}, got: {}", correctAnswer, userAnswer);
                applicationLogRepository.save(new ApplicationLog(
                        "ERROR",
                        "Math captcha failed. Expected: " + correctAnswer + ", got: " + userAnswer,
                        rqid,
                        LocalDateTime.now(),
                        loggerName
                ));
            }

            return result;

        } catch (NumberFormatException e) {
            log.error("Invalid math format. Problem: {}, Answer: {}", problem, answer);
            applicationLogRepository.save(new ApplicationLog(
                    "ERROR",
                    "Invalid math format. Problem: " + problem + ", Answer: " + answer,
                    rqid,
                    LocalDateTime.now(),
                    loggerName
            ));
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid math problem/answer format"
            );
        }
    }

}