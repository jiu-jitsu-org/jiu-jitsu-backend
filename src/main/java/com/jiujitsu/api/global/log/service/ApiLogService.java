package com.jiujitsu.api.global.log.service;

import com.jiujitsu.api.domain.log.dto.ApiLogResponse;
import com.jiujitsu.api.global.log.entity.ApiLog;
import com.jiujitsu.api.global.log.repository.ApiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiLogService {

    private final ApiLogRepository apiLogRepository;

    @Transactional
    public void save(ApiLog apiLog) {
        try {
            apiLogRepository.save(apiLog);
        } catch (Exception e) {
            log.error("API 로그 저장 실패: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<ApiLogResponse> getLogs(LocalDate date, Boolean success, Pageable pageable) {
        if (date != null && success != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            return apiLogRepository.findBySuccessAndRequestedAtBetween(success, start, end, pageable)
                    .map(ApiLogResponse::from);
        }
        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            return apiLogRepository.findByRequestedAtBetween(start, end, pageable)
                    .map(ApiLogResponse::from);
        }
        if (success != null) {
            return apiLogRepository.findBySuccess(success, pageable)
                    .map(ApiLogResponse::from);
        }
        return apiLogRepository.findAll(pageable).map(ApiLogResponse::from);
    }
}
