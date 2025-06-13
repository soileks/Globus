package com.bank.userservice.repository.Log;

import com.bank.userservice.model.Log.ApplicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationLogRepository extends JpaRepository<ApplicationLog, Long> {
    // Можно добавить кастомные методы запросов
}
