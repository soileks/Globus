package com.bank.userservice.service;

import com.bank.userservice.dto.auth.AuthRequestDto;
import com.bank.userservice.dto.auth.AuthResponseDto;
import com.bank.userservice.model.Log.ApplicationLog;
import com.bank.userservice.model.Log.IntegrationLog;
import com.bank.userservice.repository.Log.ApplicationLogRepository;
import com.bank.userservice.repository.Log.IntegrationLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationLogService {
    private final IntegrationLogRepository integrationLogRepository;
    private final ApplicationLogRepository applicationLogRepository;
    private final ObjectMapper objectMapper; // Внедряем настроенный ObjectMapper
    private final String loggerName = this.getClass().getName();

    public void logInteraction(AuthRequestDto request, AuthResponseDto response) {
        try {
            IntegrationLog logEntry = new IntegrationLog();
            logEntry.setRqid(request.getRqid());
            logEntry.setRsid(response.getRsid());
            logEntry.setRequestTime(LocalDateTime.now());
            logEntry.setResponseTime(response.getResponseTime());
            logEntry.setStatusCode(response.getStatusCode());
            logEntry.setRequestData(objectMapper.writeValueAsString(request));
            logEntry.setResponseData(objectMapper.writeValueAsString(response));

            integrationLogRepository.save(logEntry);
        } catch (JsonProcessingException e) {
            log.error("JSON conversion error. Rqid: {}. Error: {}",
                    request.getRqid(), e.getMessage());
            applicationLogRepository.save(new ApplicationLog(
                    "ERROR",
                    "JSON conversion error. Rqid: " + request.getRqid() + ". Error: " + e.getMessage(),
                    request.getRqid(),
                    LocalDateTime.now(),
                    loggerName
            ));
        }
    }

    public AuthResponseDto mapToAuthResponseDto(AuthRequestDto requestDto, Map<String, Object> response) {
        AuthResponseDto responseDto = new AuthResponseDto();
        responseDto.setRqid(requestDto.getRqid());
        responseDto.setRsid(generateRsid());
        responseDto.setStatusCode(HttpStatus.OK.value());
        responseDto.setResponse(response);
        responseDto.setResponseTime(LocalDateTime.now());
        return responseDto;
    }

    public AuthResponseDto logErrorToIntegrationLogs(AuthRequestDto request, HttpStatusCode status, String errorMessage) {
        AuthResponseDto errorResponse = new AuthResponseDto();
        try {

            errorResponse.setRqid(request.getRqid());
            errorResponse.setRsid(generateRsid());
            errorResponse.setStatusCode(status.value());
            errorResponse.setResponseTime(LocalDateTime.now());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("error", errorMessage);
            errorResponse.setResponse(responseData);

            logInteraction(request, errorResponse);
        } catch (Exception e) {
            log.error("Failed to log error to integration logs", e);
            applicationLogRepository.save(new ApplicationLog(
                    "ERROR",
                    "Failed to log error to integration logs: " + e.getMessage(),
                    request.getRqid(),
                    LocalDateTime.now(),
                    loggerName
            ));
        }
        return errorResponse;
    }

    private Long generateRsid() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }
}


