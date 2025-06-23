package com.bank.userservice.exception;
/**
 * Исключение для случая, когда истек срок действия токена
 */
public class AccountExpiredException extends RuntimeException {
    public AccountExpiredException(String message) {
        super(message);
    }
}