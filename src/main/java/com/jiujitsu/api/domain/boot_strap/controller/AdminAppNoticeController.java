package com.jiujitsu.api.domain.boot_strap.controller;

import com.jiujitsu.api.domain.boot_strap.dto.AdminAppNoticeRequest;
import com.jiujitsu.api.domain.boot_strap.dto.AdminAppNoticeResponse;
import com.jiujitsu.api.domain.boot_strap.service.AppNoticeService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.security.annotation.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "admin-app-notice-controller", description = "관리자 앱 공지 API")
@CommonApiResponses
@AdminOnly
@RestController
@RequestMapping("/admin/app-notices")
@RequiredArgsConstructor
public class AdminAppNoticeController {

    private final AppNoticeService appNoticeService;

    @Operation(summary = "앱 공지 목록 조회")
    @GetMapping
    public List<AdminAppNoticeResponse> getAll() {
        return appNoticeService.getAllNotices();
    }

    @Operation(summary = "앱 공지 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminAppNoticeResponse create(@Valid @RequestBody AdminAppNoticeRequest request) {
        return appNoticeService.create(request);
    }

    @Operation(summary = "앱 공지 수정")
    @ApiErrorCodeExamples({ErrorCode.APP_NOTICE_NOT_FOUND})
    @PutMapping("/{id}")
    public AdminAppNoticeResponse update(@PathVariable Long id,
                                         @Valid @RequestBody AdminAppNoticeRequest request) {
        return appNoticeService.update(id, request);
    }

    @Operation(summary = "앱 공지 삭제")
    @ApiErrorCodeExamples({ErrorCode.APP_NOTICE_NOT_FOUND})
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        appNoticeService.delete(id);
    }
}
