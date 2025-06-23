package com.bank.userservice.service;

import com.bank.userservice.dto.auth.RequestContext;
import com.bank.userservice.model.User;
import com.bank.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import static com.bank.userservice.model.log.enums.LogLevel.INFO;
/**
 * Сервис для выполнения запланированных задач по обслуживанию системы.
 *
 * <p>Использует Spring Scheduling для выполнения задач по расписанию.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksService {
    /** Репозиторий с сохраненными пользователями */
    private final UserRepository userRepository;
    /** Сервис для логирования приложения */
    private final ApplicationLogService applicationLogService;
    /** Контекст запроса(для хранения и получения rqid при логировании) */
    private final RequestContext requestContext;
    /**
     * Имя класса-источника лога
     */
    private final String loggerName = this.getClass().getName();

    /**
     * Очищает базу данных от неподтвержденных пользователей с истекшим сроком верификации.
     *
     * <p>Выполняется ежедневно в 3:00 по cron-расписанию.
     *
     * <p>Логика работы:
     * <ol>
     *   <li>Находит всех пользователей с неподтвержденным email</li>
     *   <li>Фильтрует тех, у кого истек срок действия токена</li>
     *   <li>Удаляет таких пользователей из системы</li>
     *   <li>Логирует результат очистки</li>
     * </ol>
     */
    @Scheduled(cron = "0 0 3 * * ?") // Каждый день в 3 ночи
    @Transactional
    public void cleanupUnverifiedUsers() {
        LocalDateTime now = LocalDateTime.now();

        List<User> expiredUsers = userRepository
                .findByEmailVerifiedFalseAndEmailVerificationTokenExpiresAtBefore(now);

        userRepository.deleteAll(expiredUsers);

        applicationLogService.log(INFO,
                "Deleted " + expiredUsers.size() + " expired unverified accounts",
                requestContext.getRqid(),
                loggerName);
    }
}
