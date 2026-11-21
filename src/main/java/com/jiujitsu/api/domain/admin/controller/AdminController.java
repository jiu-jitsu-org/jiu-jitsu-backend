package com.jiujitsu.api.domain.admin.controller;

import com.jiujitsu.api.domain.admin.dto.AdminLoginRequest;
import com.jiujitsu.api.domain.admin.dto.AdminLoginResponse;
import com.jiujitsu.api.domain.admin.dto.AdminLogoutRequest;
import com.jiujitsu.api.domain.admin.dto.AdminRefreshRequest;
import com.jiujitsu.api.domain.admin.dto.AdminSignupRequest;
import com.jiujitsu.api.domain.admin.service.AdminAuthService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin-auth", description = "관리자 인증 API")
@CommonApiResponses
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "관리자 회원가입", description = "이메일/비밀번호로 관리자 계정을 생성하고 JWT 토큰을 발급합니다.")
    @ApiErrorCodeExamples({ErrorCode.EMAIL_DUPLICATED})
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminLoginResponse signup(@Valid @RequestBody AdminSignupRequest request) {
        return adminAuthService.signup(request);
    }

    @Operation(summary = "관리자 로그인", description = "이메일/비밀번호로 로그인 후 JWT 토큰을 발급합니다.")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.AUTHENTICATION_FAILED})
    @PostMapping("/login")
    public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return adminAuthService.login(request);
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새로운 액세스·리프레시 토큰을 발급합니다.")
    @ApiErrorCodeExamples({ErrorCode.INVALID_REFRESH_TOKEN, ErrorCode.NOT_MATCH_CATEGORY, ErrorCode.USER_NOT_FOUND})
    @PostMapping("/refresh")
    public AdminLoginResponse refresh(@Valid @RequestBody AdminRefreshRequest request) {
        return adminAuthService.refresh(request);
    }

    @Operation(summary = "로그아웃", description = "액세스·리프레시 토큰을 무효화합니다.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody AdminLogoutRequest request) {
        adminAuthService.logout(request);
    }
}
