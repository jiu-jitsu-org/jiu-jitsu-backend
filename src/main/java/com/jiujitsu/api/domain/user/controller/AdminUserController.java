package com.jiujitsu.api.domain.user.controller;

import com.jiujitsu.api.domain.user.dto.AccountTypeUpdateRequest;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "admin-user-controller", description = "관리자 회원 관리 API")
@CommonApiResponses
@AdminOnly
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @Operation(summary = "계정 구분 변경", description = "회원을 일반(MEMBER) · 운영(OPERATOR) · 내부(INTERNAL) 계정으로 지정합니다. 분석에서 운영 · 내부 계정을 빼는 데 씁니다.")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND})
    @PatchMapping("/{userId}/account-type")
    public void updateAccountType(
            @Parameter(name = "userId", description = "회원 ID", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody AccountTypeUpdateRequest request
    ) {
        userService.updateAccountType(userId, request.accountType());
    }
}
