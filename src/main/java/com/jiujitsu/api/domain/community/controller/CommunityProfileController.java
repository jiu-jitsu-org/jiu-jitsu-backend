package com.jiujitsu.api.domain.community.controller;

import com.jiujitsu.api.domain.community.dto.*;
import com.jiujitsu.api.domain.community.service.CommunityProfileService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExample;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "community-profile-controller", description = "커뮤니티 프로필 API")
@ApiErrorCodeExamples({ErrorCode.WRONG_PARAMETER, ErrorCode.AUTHENTICATION_FAILED})
@RestController
@RequestMapping("/community/profile")
@RequiredArgsConstructor
public class CommunityProfileController {

    private final CommunityProfileService communityProfileService;

    @Operation(
            summary = "내 커뮤니티 프로필 조회",
            description = "현재 로그인한 사용자의 커뮤니티 프로필을 조회합니다."
    )
    @ApiErrorCodeExample(ErrorCode.USER_NOT_FOUND)
    @GetMapping
    public CommunityProfileResponse getMyProfile() {
        return communityProfileService.getMyProfile();
    }

    @Operation(
            summary = "내 커뮤니티 프로필 생성/수정22222222",
            description = "현재 로그인한 사용자의 커뮤니티 프로필을 생성하거나 수정합니다."
    )
    @ApiErrorCodeExample(ErrorCode.USER_NOT_FOUND)
    @PostMapping
    public CommunityProfileResponse upsertMyProfile(
            @Valid @RequestBody CommunityProfileRequest request
    ) {
        return communityProfileService.upsertMyProfile(request);
    }
}
