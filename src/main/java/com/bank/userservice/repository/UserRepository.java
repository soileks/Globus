package com.bank.userservice.repository;

import com.bank.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с пользователями в базе данных.
 *
 * <p>Наследует стандартные методы JpaRepository и добавляет специализированные:
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Находит пользователя по имени.
     *
     * @param username имя пользователя
     * @return Optional с пользователем, если найден
     */
    Optional<User> findByUsername(String username);

    /**
     * Находит пользователя по email.
     *
     * @param email адрес электронной почты
     * @return Optional с пользователем, если найден
     */
    Optional<User> findByEmail(String email);

    /**
     * Проверяет существование пользователя по имени или email.
     *
     * @param username имя пользователя
     * @param email адрес электронной почты
     * @return true если пользователь существует
     */
    boolean existsByUsernameOrEmail(String username, String email);

    /**
     * Находит всех неподтвержденных пользователей с истекшим сроком верификации.
     *
     * @param date граничная дата истечения срока
     * @return список пользователей
     */
    List<User> findByEmailVerifiedFalseAndEmailVerificationTokenExpiresAtBefore(LocalDateTime date);
}