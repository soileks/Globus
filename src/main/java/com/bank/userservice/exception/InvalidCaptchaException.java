package com.bank.userservice.exception;

/**
 * Исключение для ошибок, связанных с капчей
 */
public class InvalidCaptchaException extends RuntimeException {
    public InvalidCaptchaException(String message) {
        super(message);
    }
}
