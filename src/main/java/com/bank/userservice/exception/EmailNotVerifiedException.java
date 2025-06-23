package com.bank.userservice.exception;
/**
 * Исключение для случая, когда срок действия токена не истек и email е подтвержден
 */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}