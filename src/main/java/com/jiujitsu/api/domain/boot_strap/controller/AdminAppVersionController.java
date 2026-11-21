package com.jiujitsu.api.domain.boot_strap.controller;

import com.jiujitsu.api.domain.boot_strap.dto.AdminAppVersionResponse;
import com.jiujitsu.api.domain.boot_strap.dto.AdminAppVersionUpsertRequest;
import com.jiujitsu.api.domain.boot_strap.entity.OsType;
import com.jiujitsu.api.domain.boot_strap.service.AppVersionService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "admin-app-version-controller", description = "관리자 앱 버전 API")
@CommonApiResponses
@RestController
@RequestMapping("/admin/app-versions")
@RequiredArgsConstructor
public class AdminAppVersionController {

    private final AppVersionService appVersionService;

    @Operation(summary = "앱 버전 목록 조회",
            description = "IOS · ANDROID 두 OS의 최소·최신 버전과 마지막 수정 시각을 반환합니다. (ROLE_ADMIN 필요)")
    @GetMapping
    public List<AdminAppVersionResponse> getAllVersions() {
        return appVersionService.getAllVersions();
    }

    @Operation(summary = "앱 버전 등록·수정",
            description = "OS별 최소·최신 버전을 저장합니다. 없으면 만들고, 있으면 수정합니다. "
                    + "버전은 숫자와 점(.)으로 구성(예: 1.2.0)하며 최소 버전은 최신 버전을 초과할 수 없습니다. (ROLE_ADMIN 필요)")
    @ApiErrorCodeExamples({ErrorCode.APP_VERSION_MIN_EXCEEDS_NOW})
    @PutMapping("/{osName}")
    public AdminAppVersionResponse upsertVersion(
            @Parameter(description = "OS 종류", example = "IOS") @PathVariable OsType osName,
            @Valid @RequestBody AdminAppVersionUpsertRequest request) {
        return appVersionService.upsertVersion(osName, request);
    }
}
