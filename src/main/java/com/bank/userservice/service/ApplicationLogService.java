package com.bank.userservice.service;

import com.bank.userservice.model.log.ApplicationLog;
import com.bank.userservice.model.log.enums.LogLevel;
import com.bank.userservice.repository.log.ApplicationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
/**
 * Сервис для логирования сообщений в систему.
 *
 * <p>Обеспечивает двунаправленное логирование:
 * <ul>
 *   <li>В консоль приложения (через Slf4j)</li>
 *   <li>В базу данных (через ApplicationLogRepository)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationLogService {
    private final ApplicationLogRepository applicationLogRepository;

    /**
     * Логирует сообщение в консоль и БД
     *
     * @param level      Уровень логирования
     * @param message    Сообщение для логирования
     * @param rqid       ID запроса
     * @param loggerName Имя логгера (класса)
     */
    public void log(LogLevel level, String message, String rqid, String loggerName) {
        logToConsole(level, message);

        logToDatabase(level, message, rqid, loggerName);
    }
    /**
     * Логирует сообщение в консоль приложения.
     *
     * @param level уровень логирования
     * @param logMessage текст сообщения
     */
    private void logToConsole(LogLevel level, String logMessage) {

        switch (level) {
            case ERROR -> log.error(logMessage);
            case WARN -> log.warn(logMessage);
            case INFO -> log.info(logMessage);
            case DEBUG -> log.debug(logMessage);
            case TRACE -> log.trace(logMessage);
        }
    }
    /**
     * Сохраняет лог в базу данных.
     *
     * @param level уровень логирования
     * @param message текст сообщения
     * @param rqid идентификатор запроса
     * @param loggerName имя источника лога
     */
    private void logToDatabase(LogLevel level, String message, String rqid, String loggerName) {

        ApplicationLog logEntry = new ApplicationLog(
                level,
                message,
                rqid,
                LocalDateTime.now(),
                loggerName
        );
        applicationLogRepository.save(logEntry);

    }
}