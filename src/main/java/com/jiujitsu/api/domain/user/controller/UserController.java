package com.jiujitsu.api.domain.user.controller;

import com.jiujitsu.api.domain.user.dto.*;
import com.jiujitsu.api.domain.user.service.UserService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExample;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "user-controller", description = "사용자 정보 API")
@ApiErrorCodeExamples({ErrorCode.WRONG_PARAMETER})
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // todo: 약관동의 업데이트
    @Operation(
            summary = "회원 가입",
            description = "인증정보, 닉네임, SNS 정보를 저장합니다."
    )
    @ApiErrorCodeExamples({ErrorCode.NICKNAME_DUPLICATED, ErrorCode.NICKNAME_VALIDATION, ErrorCode.NOT_MATCH_CATEGORY})
    @PostMapping
    public AuthResponse createUser(@Valid @RequestBody CreateProfileRequest createProfileRequest) {
        return userService.createUser(createProfileRequest);
    }

    @Operation(
            summary = "사용자 프로필 조회",
            description = "현재 로그인한 사용자의 프로필 정보를 조회합니다."
    )
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.AUTHENTICATION_FAILED})
    @GetMapping("/profile")
    public UserProfileResponse getUserProfile() {
        return userService.getUserProfile();
    }

    @Operation(
            summary = "사용자 프로필 업데이트",
            description = "현재 로그인한 사용자의 닉네임과 프로필 이미지를 업데이트합니다."
    )
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.AUTHENTICATION_FAILED})
    @PutMapping("/profile")
    public UpdateProfileResponse updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(request);
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자의 계정을 비활성화합니다. 30일 이내에 로그인하면 계정을 복구할 수 있습니다."
    )
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.AUTHENTICATION_FAILED, ErrorCode.USER_ALREADY_DEACTIVATED})
    @DeleteMapping
    public Boolean deactivateUser() {
        userService.deactivateUser();
        return true;
    }

    @Operation(
            summary = "(관리자로 옮길거임) 관장/사범 권한 부여",
            description = "사용자에게 관장/사범 권한을 부여합니다."
    )
    @ApiErrorCodeExample(ErrorCode.USER_NOT_FOUND)
    @PutMapping("/grant/owner-role")
    public UserProfileResponse grantOwnerRole() {
        return userService.grantOwnerRole();
    }

    @Operation(
            summary = "닉네임 유효성 체크",
            description = "중복되거나 유효하지 않은 닉네임인지 확인합니다."
    )
    @ApiErrorCodeExamples({ErrorCode.NICKNAME_DUPLICATED, ErrorCode.NICKNAME_VALIDATION})
    @GetMapping("/check/nickname")
    public Boolean duplicateNickname(@RequestParam(value = "nickname") String nickname) {
        return userService.checkNickname(nickname);
    }
}
