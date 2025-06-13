package com.bank.userservice.model.Log;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "integration_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long rqid;

    @Column(nullable = false)
    private Long rsid;

    @Column(nullable = false)
    private LocalDateTime requestTime;

    @Column(nullable = false)
    private LocalDateTime responseTime;

    @Column(nullable = false)
    private Integer statusCode;

    @Column(columnDefinition = "TEXT")
    private String requestData;

    @Column(columnDefinition = "TEXT")
    private String responseData;
}
