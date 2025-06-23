package com.bank.userservice.repository.log;

import com.bank.userservice.model.log.ApplicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
/**
 * Репозиторий для работы с логами приложения.
 *
 * <p>Наследует стандартные методы JpaRepository.
 */
@Repository
public interface ApplicationLogRepository extends JpaRepository<ApplicationLog, Long> {
    // Можно добавить кастомные методы запросов
}
