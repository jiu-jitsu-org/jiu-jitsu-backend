package com.jiujitsu.api.domain.user.controller;

import com.jiujitsu.api.domain.user.dto.AuthResponse;
import com.jiujitsu.api.domain.user.dto.SnsLoginRequest;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "카카오 로그인",
            description = "카카오 SDK에서 받은 액세스 토큰으로 로그인합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content)
    })
    @PostMapping("/kakao")
    public ResponseEntity<AuthResponse> kakaoLogin(
            @Parameter(description = "카카오 액세스 토큰", required = true)
            @RequestParam String accessToken) {
        
        SnsLoginRequest request = new SnsLoginRequest(SnsProvider.KAKAO, accessToken, null);
        AuthResponse response = authService.snsLogin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "구글 로그인",
            description = "구글 SDK에서 받은 액세스 토큰으로 로그인합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content)
    })
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(
            @Parameter(description = "구글 액세스 토큰", required = true)
            @RequestParam String accessToken) {
        
        SnsLoginRequest request = new SnsLoginRequest(SnsProvider.GOOGLE, accessToken, null);
        AuthResponse response = authService.snsLogin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "애플 로그인",
            description = "애플 SDK에서 받은 ID 토큰으로 로그인합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content)
    })
    @PostMapping("/apple")
    public ResponseEntity<AuthResponse> appleLogin(
            @Parameter(description = "애플 ID 토큰", required = true)
            @RequestParam String idToken) {
        
        SnsLoginRequest request = new SnsLoginRequest(SnsProvider.APPLE, null, idToken);
        AuthResponse response = authService.snsLogin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "통합 SNS 로그인",
            description = "SNS 제공자와 토큰 정보를 포함한 통합 로그인 API입니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content)
    })
    @PostMapping("/sns-login")
    public ResponseEntity<AuthResponse> snsLogin(
            @Valid @RequestBody SnsLoginRequest request) {
        
        AuthResponse response = authService.snsLogin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "토큰 갱신",
            description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰",
                    content = @Content)
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Parameter(description = "리프레시 토큰", required = true)
            @RequestParam String refreshToken) {
        
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
}
