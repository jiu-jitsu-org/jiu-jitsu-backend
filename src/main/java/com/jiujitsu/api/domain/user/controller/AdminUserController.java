package com.jiujitsu.api.domain.user.controller;

import com.jiujitsu.api.domain.user.dto.*;
import com.jiujitsu.api.domain.user.service.AdminUserService;
import com.jiujitsu.api.domain.user.service.UserService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.security.annotation.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "admin-user-controller", description = "관리자 회원 관리 API")
@CommonApiResponses
@AdminOnly
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final AdminUserService adminUserService;

    // ── 회원 목록 ──────────────────────────────────────────────────

    @Operation(summary = "회원 목록", description = "닉네임·이메일 검색, 상태·제재 필터, 가입일·신고 수·위반 수 정렬. 20개씩.")
    @GetMapping
    public Page<AdminUserListItem> getUsers(
            @ModelAttribute AdminUserListRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminUserService.getUsers(request, page, size);
    }

    // ── 회원 상세 ──────────────────────────────────────────────────

    @Operation(summary = "회원 상세", description = "기본 정보, 관심 대회, 대회 기록, 제재 이력을 반환합니다.")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND})
    @GetMapping("/{userId}")
    public AdminUserDetailResponse getUser(
            @Parameter(description = "회원 ID") @PathVariable Long userId
    ) {
        return adminUserService.getUser(userId);
    }

    // ── 계정 구분 변경 ─────────────────────────────────────────────

    @Operation(summary = "계정 구분 변경", description = "회원을 일반(MEMBER) · 운영(OPERATOR) · 내부(INTERNAL) 계정으로 지정합니다.")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND})
    @PatchMapping("/{userId}/account-type")
    public void updateAccountType(
            @Parameter(description = "회원 ID") @PathVariable Long userId,
            @Valid @RequestBody AccountTypeUpdateRequest request
    ) {
        userService.updateAccountType(userId, request.accountType());
    }

    // ── 제재 처리 ──────────────────────────────────────────────────

    @Operation(summary = "제재 처리", description = "경고·일시정지·영구차단을 처리합니다. 일시정지는 durationDays 필수.")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.SANCTION_DURATION_REQUIRED})
    @PostMapping("/{userId}/sanctions")
    public void applySanction(
            @Parameter(description = "회원 ID") @PathVariable Long userId,
            @Valid @RequestBody AdminSanctionRequest request
    ) {
        adminUserService.applySanction(userId, request);
    }

    // ── 제재 해제 ──────────────────────────────────────────────────

    @Operation(summary = "제재 해제", description = "활성 제재를 해제합니다. 사유 필수.")
    @ApiErrorCodeExamples({ErrorCode.SANCTION_NOT_FOUND, ErrorCode.SANCTION_ALREADY_RELEASED})
    @DeleteMapping("/{userId}/sanctions/{sanctionId}")
    public void releaseSanction(
            @Parameter(description = "회원 ID") @PathVariable Long userId,
            @Parameter(description = "제재 ID") @PathVariable Long sanctionId,
            @Valid @RequestBody AdminSanctionReleaseRequest request
    ) {
        adminUserService.releaseSanction(userId, sanctionId, request);
    }

    // ── 운영자 대신 탈퇴 처리 ─────────────────────────────────────

    @Operation(summary = "운영자 대신 탈퇴 처리", description = "유저가 앱에 접근하지 못해 문의로 탈퇴를 요청한 경우 사용합니다.")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.USER_ALREADY_DEACTIVATED})
    @PostMapping("/{userId}/withdrawal")
    public void withdrawOnBehalf(
            @Parameter(description = "회원 ID") @PathVariable Long userId
    ) {
        adminUserService.withdrawOnBehalf(userId);
    }
}
