package com.jiujitsu.api.domain.admin.controller;

import com.jiujitsu.api.domain.admin.dto.AdminLoginRequest;
import com.jiujitsu.api.domain.admin.dto.AdminLoginResponse;
import com.jiujitsu.api.domain.admin.dto.AdminSignupRequest;
import com.jiujitsu.api.domain.admin.service.AdminAuthService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin-controller", description = "관리자 API")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "관리자 회원가입", description = "이메일/비밀번호로 관리자 계정을 생성하고 JWT 토큰을 발급합니다.")
    @ApiErrorCodeExamples({ErrorCode.NICKNAME_DUPLICATED, ErrorCode.WRONG_PARAMETER})
    @PostMapping("/auth/signup")
    public AdminLoginResponse signup(@Valid @RequestBody AdminSignupRequest request) {
        return adminAuthService.signup(request);
    }

    @Operation(summary = "관리자 로그인", description = "이메일/비밀번호로 관리자 로그인 후 JWT 토큰을 발급합니다.")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.AUTHENTICATION_FAILED})
    @PostMapping("/auth/login")
    public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return adminAuthService.login(request);
    }
}
