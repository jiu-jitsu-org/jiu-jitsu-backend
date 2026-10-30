package com.jiujitsu.api.domain.community.profile.controller;

import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileRequest;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileResponse;
import com.jiujitsu.api.domain.community.profile.dto.CommunityPublicProfileResponse;
import com.jiujitsu.api.domain.community.profile.service.CommunityProfileService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.exception.annotation.LoginErrorExamples;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "community-profile-controller", description = "커뮤니티 프로필 API")
@CommonApiResponses
@RestController
@RequestMapping("/community/profile")
@RequiredArgsConstructor
public class CommunityProfileController {

    private final CommunityProfileService communityProfileService;

    @Operation(
            summary = "내 커뮤니티 프로필(선수 카드) 조회",
            description = "현재 로그인한 사용자의 커뮤니티 프로필을 조회합니다."
    )
    @LoginErrorExamples
    @GetMapping
    public CommunityProfileResponse getMyProfile() {
        return communityProfileService.getMyProfile();
    }

    @Operation(
            summary = "다른 사용자 선수 카드 조회",
            description = "특정 사용자의 선수 카드를 조회합니다. 비로그인 가능. 차단 관계이면 404 반환. isWeightHidden=true 인 사용자의 체급은 null로 마스킹됩니다."
    )
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND})
    @GetMapping("/{userId}")
    public CommunityProfileResponse getProfile(
            @Parameter(name = "userId", description = "조회할 사용자 ID", required = true)
            @PathVariable Long userId
    ) {
        return communityProfileService.getProfile(userId);
    }

    @Operation(
            summary = "내 커뮤니티 프로필 생성/수정",
            description = "현재 로그인한 사용자의 커뮤니티 프로필을 생성하거나 수정합니다."
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.PERMISSION_DENIED, ErrorCode.REQUIRED_PARAMETER,
            ErrorCode.COMPETITION_PIN_LIMIT_EXCEEDED, ErrorCode.COMPETITION_DUPLICATED,
            ErrorCode.COMPETITION_NOT_FOUND, ErrorCode.WRONG_PARAMETER})
    @PostMapping
    public CommunityProfileResponse upsertMyProfile(
            @Valid @RequestBody CommunityProfileRequest request
    ) {
        return communityProfileService.upsertMyProfile(request);
    }
}
