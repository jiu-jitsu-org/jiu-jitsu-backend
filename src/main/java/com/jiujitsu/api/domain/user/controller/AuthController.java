package com.jiujitsu.api.domain.user.controller;

import com.jiujitsu.api.domain.user.dto.AuthResponse;
import com.jiujitsu.api.domain.user.dto.LogoutRequest;
import com.jiujitsu.api.domain.user.dto.LogoutResponse;
import com.jiujitsu.api.domain.user.dto.SnsLoginRequest;
import com.jiujitsu.api.domain.user.service.AuthService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "authentication-controller", description = "인증 API")
@ApiErrorCodeExamples({ErrorCode.WRONG_PARAMETER, ErrorCode.AUTHENTICATION_FAILED})
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "통합 SNS 로그인",
            description = "SNS 제공자와 토큰 정보를 포함한 통합 로그인 API입니다."
    )
    @PostMapping("/sns-login")
    public AuthResponse snsLogin(
            @Valid @RequestBody SnsLoginRequest request) {

        return authService.snsLogin(request);
    }

    @Operation(
            summary = "토큰 갱신",
            description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다."
    )
    @ApiErrorCodeExamples({ErrorCode.INVALID_REFRESH_TOKEN, ErrorCode.NOT_MATCH_CATEGORY, ErrorCode.USER_NOT_FOUND})
    @PostMapping("/refresh")
    public AuthResponse refreshToken(
            @Parameter(description = "리프레시 토큰", required = true)
            @RequestParam String refreshToken) {

        return authService.refreshToken(refreshToken);
    }

    @Operation(
            summary = "로그아웃",
            description = "액세스 토큰과 리프레시 토큰을 무효화하여 로그아웃합니다."
    )
    @PostMapping("/logout")
    public LogoutResponse logout(
            @Valid @RequestBody LogoutRequest request) {

        return authService.logout(request);
    }
}
