package com.jiujitsu.api.global.log.service;

import com.jiujitsu.api.domain.log.dto.ApiLogResponse;
import com.jiujitsu.api.global.log.entity.ApiLog;
import com.jiujitsu.api.global.log.repository.ApiLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ApiLogService {

    private final ApiLogRepository apiLogRepository;

    @Transactional
    public void save(ApiLog apiLog) {
        apiLogRepository.save(apiLog);
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
