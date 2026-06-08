package com.jiujitsu.api.domain.log.controller;

import com.jiujitsu.api.domain.log.dto.ApiLogResponse;
import com.jiujitsu.api.global.exception.ApiResponse;
import com.jiujitsu.api.global.log.service.ApiLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "log-controller", description = "API 로그 조회 (관리자 전용)")
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
public class ApiLogController {

    private final ApiLogService apiLogService;

    @Operation(summary = "API 로그 목록 조회", description = "날짜 및 성공 여부로 필터링 가능한 API 호출 로그를 페이지 단위로 조회합니다.")
    @GetMapping
    public ApiResponse<Page<ApiLogResponse>> getLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Boolean success,
            @PageableDefault(size = 20, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(apiLogService.getLogs(date, success, pageable));
    }
}
