package com.bank.userservice.model.log;
import com.bank.userservice.model.log.enums.LogLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
/**
 * Сущность для логов приложения.
 */
@Entity
@Table(name = "application_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationLog {
    /** Уникальный идентификатор */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Уровень логирования (INFO, WARN, ERROR и т.д.) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogLevel level;

    /** Текст сообщения (макс. 1000 символов) */
    @Column(nullable = false, length = 1000)
    private String message;

    /** Идентификатор запроса */
    @Column(nullable = false)
    private String rqid;

    /** Временная метка события */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /** Класс-источник лога (макс. 100 символов) */
    @Column(nullable = false, length = 100)
    private String logger;

    /**
     * Конструктор для создания лога.
     * @param level уровень логирования
     * @param message текст сообщения
     * @param rqid идентификатор запроса
     * @param timestamp временная метка события
     * @param logger класс-источник лога
     */
    public ApplicationLog(LogLevel level, String message, String rqid, LocalDateTime timestamp, String logger) {
        this.level = level;
        this.message = message;
        this.rqid = rqid;
        this.timestamp = timestamp;
        this.logger = logger;
    }
}