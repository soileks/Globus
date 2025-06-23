package com.bank.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Этот класс является точкой входа в приложение Spring Boot
 */
@SpringBootApplication
@EnableScheduling
public class UserServiceApplication {
    /**
     * Метод main, который запускает приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}