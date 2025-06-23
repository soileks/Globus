package com.bank.userservice.repository.log;

import com.bank.userservice.model.log.IntegrationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
/**
 * Репозиторий для работы с интеграционными логами.
 *
 * <p>Наследует стандартные методы JpaRepository.
 */
@Repository
public interface IntegrationLogRepository extends JpaRepository<IntegrationLog, Long> {
    // Можно добавить кастомные методы запросов
}
