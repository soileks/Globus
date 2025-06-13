package com.bank.userservice.model.Log;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "application_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class ApplicationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String level; // INFO, WARN, ERROR

    @Column(nullable = false, length = 1000)
    private String message;

    private Long rqid;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 100)
    private String logger; // Класс, из которого пришло сообщение

    public ApplicationLog(String level, String message, Long rqid, LocalDateTime timestamp, String logger) {
        this.level = level;
        this.message = message;
        this.rqid = rqid;
        this.timestamp = timestamp;
        this.logger = logger;
    }
}