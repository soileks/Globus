package com.bank.userservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Сущность пользователя системы.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    /** Уникальный идентификатор */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Уникальное имя пользователя */
    @Column(nullable = false, unique = true)
    private String username;

    /** Уникальный email */
    @Column(nullable = false, unique = true)
    private String email;

    /** Зашифрованный пароль */
    @Column(nullable = false)
    private String password;

    /** Дата создания (автоматически устанавливается) */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Дата обновления (автоматически обновляется) */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Флаг подтверждения email */
    @Column(nullable = false)
    private boolean emailVerified = false;

    /** Токен для верификации email */
    private String emailVerificationToken;

    /** Срок действия токена верификации */
    private LocalDateTime emailVerificationTokenExpiresAt;
}