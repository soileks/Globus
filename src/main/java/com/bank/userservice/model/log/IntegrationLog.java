package com.bank.userservice.model.log;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Сущность для логов интеграционных запросов.
 */
@Entity
@Table(name = "integration_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationLog {
    /** Уникальный идентификатор */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Идентификатор запроса */
    @Column(nullable = false)
    private String rqid;

    /** Идентификатор ответа */
    @Column(nullable = false)
    private String rsid;

    /** Время отправки запроса */
    @Column(nullable = false)
    private LocalDateTime requestTime;

    /** Время получения ответа */
    @Column(nullable = false)
    private LocalDateTime responseTime;

    /** HTTP статус код ответа */
    @Column(nullable = false)
    private Integer statusCode;

    /** Данные ответа (хранятся как TEXT) */
    @Column(columnDefinition = "TEXT")
    private String responseData;
}
