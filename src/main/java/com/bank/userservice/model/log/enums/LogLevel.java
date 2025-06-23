package com.bank.userservice.model.log.enums;

/**
 * enum для уровней логирования
 */
public enum LogLevel {
    /** Для детальной отладки */
    TRACE,
    /** Отладочная информация */
    DEBUG,
    /** Информационные сообщения */
    INFO,
    /** Предупреждения */
    WARN,
    /** Ошибки */
    ERROR,
    /** Критические ошибки */
    FATAL;
}
