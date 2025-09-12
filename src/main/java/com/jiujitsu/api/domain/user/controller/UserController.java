package com.jiujitsu.api.domain.user.controller;

import com.jiujitsu.api.domain.user.dto.UpdateProfileRequest;
import com.jiujitsu.api.domain.user.dto.UpdateProfileResponse;
import com.jiujitsu.api.domain.user.dto.UserProfileResponse;
import com.jiujitsu.api.domain.user.service.UserService;
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

@Tag(name = "User", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "사용자 프로필 조회",
            description = "현재 로그인한 사용자의 프로필 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content)
    })
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 업데이트 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UpdateProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content)
    })
    @PutMapping("/profile")
    public ResponseEntity<UpdateProfileResponse> updateProfile(
            @Parameter(description = "Authorization 헤더의 Bearer 토큰", required = true)
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        UpdateProfileResponse response = userService.updateProfile(authorizationHeader, request);
        return ResponseEntity.ok(response);
    }
}
