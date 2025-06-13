package com.bank.userservice.repository.Log;

import com.bank.userservice.model.Log.IntegrationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntegrationLogRepository extends JpaRepository<IntegrationLog, Long> {
    // Можно добавить кастомные методы запросов
}
