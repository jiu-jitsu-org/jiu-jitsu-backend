package com.jiujitsu.api.domain.user.controller;

import com.jiujitsu.api.domain.user.dto.UpdateProfileRequest;
import com.jiujitsu.api.domain.user.dto.UpdateProfileResponse;
import com.jiujitsu.api.domain.user.dto.UserProfileResponse;
import com.jiujitsu.api.domain.user.service.UserService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExample;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 정보 API")
@ApiErrorCodeExamples({ErrorCode.WRONG_PARAMETER, ErrorCode.AUTHENTICATION_FAILED})
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "사용자 프로필 조회",
            description = "현재 로그인한 사용자의 프로필 정보를 조회합니다."
    )
    @ApiErrorCodeExample(ErrorCode.USER_NOT_FOUND)
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @Parameter(description = "Authorization 헤더의 Bearer 토큰", required = true)
            @RequestHeader("Authorization") String authorizationHeader) {
        
        UserProfileResponse response = userService.getUserProfile(authorizationHeader);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "사용자 프로필 업데이트",
            description = "현재 로그인한 사용자의 닉네임과 프로필 이미지를 업데이트합니다."
    )
    @ApiErrorCodeExample(ErrorCode.USER_NOT_FOUND)
    @PutMapping("/profile")
    public ResponseEntity<UpdateProfileResponse> updateProfile(
            @Parameter(description = "Authorization 헤더의 Bearer 토큰", required = true)
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        UpdateProfileResponse response = userService.updateProfile(authorizationHeader, request);
        return ResponseEntity.ok(response);
    }
}
