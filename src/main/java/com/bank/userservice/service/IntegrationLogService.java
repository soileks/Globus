package com.bank.userservice.service;

import com.bank.userservice.dto.auth.AuthResponseDto;
import com.bank.userservice.dto.auth.RequestContext;
import com.bank.userservice.model.log.IntegrationLog;
import com.bank.userservice.repository.log.ApplicationLogRepository;
import com.bank.userservice.repository.log.IntegrationLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * Сервис для логирования интеграционных взаимодействий между системами.
 *
 * <p>Основные функции:
 * <ul>
 *   <li>Логирование успешных операций</li>
 *   <li>Логирование ошибок</li>
 *   <li>Генерация уникальных идентификаторов ответов</li>
 *   <li>Сохранение полного контекста взаимодействия</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Data
public class IntegrationLogService {
    // Репозиторий интеграционных логов
    private final IntegrationLogRepository integrationLogRepository;
    // Репозиторий логов приложения
    private final ApplicationLogRepository applicationLogRepository;
    private final ObjectMapper objectMapper; // Внедряем настроенный ObjectMapper
    private final RequestContext requestContext;

    /**
     * Логирует успешное взаимодействие между системами.
     *
     * <p>Сохраняет в лог:
     * <ul>
     *   <li>Идентификаторы запроса и ответа</li>
     *   <li>Временные метки</li>
     *   <li>HTTP статус</li>
     *   <li>Данные ответа в JSON формате</li>
     * </ul>
     *
     * @param response данные ответа в виде Map
     * @return AuthResponseDto сформированный ответ
     * @throws JsonProcessingException при ошибках сериализации данных
     */
    public AuthResponseDto  logInteraction(Map<String, Object> response) throws JsonProcessingException {
        String rqid = requestContext.getRqid();
        AuthResponseDto authResponseDto = mapToAuthResponseDto(response);

        IntegrationLog logEntry = new IntegrationLog();
        logEntry.setRqid(rqid);
        logEntry.setRsid(generateRsid());
        logEntry.setRequestTime(LocalDateTime.now());
        logEntry.setResponseTime(LocalDateTime.now());
        logEntry.setStatusCode(HttpStatus.OK.value());
        //logEntry.setRequestData(objectMapper.writeValueAsString(request));
        logEntry.setResponseData(objectMapper.writeValueAsString(authResponseDto));

        integrationLogRepository.save(logEntry);
        return authResponseDto;

    }

    /**
     * Преобразует данные ответа в DTO формата AuthResponseDto.
     *
     * @param response данные ответа в виде Map
     * @return AuthResponseDto сформированный DTO ответа
     */
    public AuthResponseDto mapToAuthResponseDto(Map<String, Object> response) {
        String rqid = requestContext.getRqid();
        AuthResponseDto responseDto = new AuthResponseDto();
        responseDto.setRqid(rqid);
        responseDto.setRsid(generateRsid());
        responseDto.setStatusCode(HttpStatus.OK.value());
        responseDto.setResponse(response);
        responseDto.setResponseTime(LocalDateTime.now());
        return responseDto;
    }

    /**
     * Логирует ошибочное взаимодействие между системами.
     *
     * @param status HTTP статус ошибки
     * @param errorMessage сообщение об ошибке
     * @return AuthResponseDto с описанием ошибки
     * @throws JsonProcessingException при ошибках сериализации данных
     */
    public AuthResponseDto logErrorToIntegrationLogs(HttpStatusCode status, String errorMessage) throws JsonProcessingException {
        String rqid = requestContext.getRqid();
        AuthResponseDto errorResponseDto = new AuthResponseDto();

        errorResponseDto.setRqid(rqid);
        errorResponseDto.setRsid(generateRsid());
        errorResponseDto.setStatusCode(status.value());
        errorResponseDto.setResponseTime(LocalDateTime.now());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("error", errorMessage);
        errorResponseDto.setResponse(responseData);

        IntegrationLog logEntry = new IntegrationLog();
        logEntry.setRqid(rqid);
        logEntry.setRsid(errorResponseDto.getRsid());
        logEntry.setRequestTime(LocalDateTime.now());
        logEntry.setResponseTime(errorResponseDto.getResponseTime());
        logEntry.setStatusCode(errorResponseDto.getStatusCode());
        //logEntry.setRequestData(objectMapper.writeValueAsString(request));
        logEntry.setResponseData(objectMapper.writeValueAsString(errorResponseDto));

        integrationLogRepository.save(logEntry);

        return errorResponseDto;
    }

    /**
     * Генерирует уникальный идентификатор ответа (RSID).
     *
     * <p>Формат: RES_[временная метка]_[UUID]
     *
     * @return уникальный идентификатор ответа
     */
    public static String generateRsid() {
        return "RES_" + LocalDateTime.now() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}


