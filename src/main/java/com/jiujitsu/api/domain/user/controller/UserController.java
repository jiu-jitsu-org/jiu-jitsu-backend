package com.jiujitsu.api.domain.user.controller;

import com.jiujitsu.api.domain.user.dto.AuthResponse;
import com.jiujitsu.api.domain.user.dto.CreateProfileRequest;
import com.jiujitsu.api.domain.user.dto.UserAppInfoRequest;
import com.jiujitsu.api.domain.user.dto.UserProfileResponse;
import com.jiujitsu.api.domain.user.service.UserService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.exception.annotation.LoginErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "user-controller", description = "사용자 정보 API")
@CommonApiResponses
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "회원 가입",
            description = "인증정보, 닉네임, SNS 정보를 저장합니다."
    )
    @ApiErrorCodeExamples({ErrorCode.NICKNAME_DUPLICATED, ErrorCode.NICKNAME_VALIDATION})
    @PostMapping
    public AuthResponse createUser(@Valid @RequestBody CreateProfileRequest createProfileRequest) {
        return userService.createUser(createProfileRequest);
    }

    @Operation(
            summary = "사용자 프로필 조회",
            description = "현재 로그인한 사용자의 프로필 정보를 조회합니다."
    )
    @LoginErrorExamples
    @GetMapping("/profile")
    public UserProfileResponse getUserProfile() {
        return userService.getUserProfile();
    }

    @Operation(
            summary = "사용자 닉네임 업데이트",
            description = "현재 로그인한 사용자의 닉네임을 업데이트합니다."
    )
    @LoginErrorExamples
    @PutMapping("/profile/nickname")
    public UserProfileResponse updateNickname(
            @RequestParam(value = "nickname") String nickname) {
        return userService.updateProfileNickname(nickname);
    }

    @Operation(
            summary = "사용자 프로필 이미지 업데이트",
            description = "현재 로그인한 사용자의 프로필 이미지를 업데이트합니다."
    )
    @LoginErrorExamples
    @PutMapping("/profile/image")
    public UserProfileResponse updateProfileImage(
            @RequestParam(value = "imageFileId") Long imageFileId) {
        return userService.updateProfileImage(imageFileId);
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자의 계정을 비활성화합니다. 30일 이내에 로그인하면 계정을 복구할 수 있습니다."
    )
    @LoginErrorExamples
    @DeleteMapping
    public Boolean deactivateUser() {
        userService.deactivateUser();
        return true;
    }

    @Operation(
            summary = "닉네임 유효성 체크",
            description = "중복되거나 유효하지 않은 닉네임인지 확인합니다."
    )
    @ApiErrorCodeExamples({ErrorCode.NICKNAME_DUPLICATED, ErrorCode.NICKNAME_VALIDATION})
    @GetMapping("/check/nickname")
    public Boolean duplicateNickname(@RequestParam(value = "nickname") String nickname) {
        return userService.validateNickname(nickname);
    }

    @Operation(
            summary = "회원 앱 정보 등록",
            description = "로그인 회원의 앱 정보를 등록합니다."
    )
    @LoginErrorExamples
    @PostMapping("/appInfo")
    public Boolean insertUserAppInfo(@RequestBody UserAppInfoRequest request) {
        return userService.insertUserAppInfo(request);
    }

    @Operation(
            summary = "관장/사범 인증 요청",
            description = "관장/사범 인증 이미지를 업로드하고 권한 요청합니다."
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.PERMISSION_DENIED, ErrorCode.IMAGE_FILE_NOT_FOUND})
    @PutMapping("/owner")
    public UserProfileResponse requestOwner(@RequestParam(name = "imageFileId") Long imageFileId) {
        return userService.requestOwnerRole(imageFileId);
    }
}
