package com.jiujitsu.api.domain.community.profile.controller;

import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileRequest;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileResponse;
import com.jiujitsu.api.domain.community.profile.dto.CommunityPublicProfileResponse;
import com.jiujitsu.api.domain.community.profile.service.CommunityProfileService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.exception.annotation.LoginErrorExamples;
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
            summary = "내 커뮤니티 프로필 조회",
            description = "현재 로그인한 사용자의 커뮤니티 프로필을 조회합니다."
    )
    @LoginErrorExamples
    @GetMapping
    public CommunityProfileResponse getMyProfile() {
        return communityProfileService.getMyProfile();
    }

    @Operation(
            summary = "다른 회원 공개 프로필 조회",
            description = "회원 ID로 공개 프로필을 조회합니다. 비로그인도 조회 가능하며, 체급·성별·특기·지도 정보는 내려가지 않습니다. 대회 정보는 대회명이 있는 기록만 내려갑니다."
    )
    @ApiErrorCodeExamples({ErrorCode.MEMBER_NOT_FOUND})
    @GetMapping("/{userId}")
    public CommunityPublicProfileResponse getPublicProfile(@PathVariable Long userId) {
        return communityProfileService.getPublicProfile(userId);
    }

    @Operation(
            summary = "내 커뮤니티 프로필 생성/수정",
            description = "현재 로그인한 사용자의 커뮤니티 프로필을 생성하거나 수정합니다."
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.PERMISSION_DENIED, ErrorCode.REQUIRED_PARAMETER,
            ErrorCode.COMPETITION_PIN_LIMIT_EXCEEDED, ErrorCode.COMPETITION_DUPLICATED})
    @PostMapping
    public CommunityProfileResponse upsertMyProfile(
            @Valid @RequestBody CommunityProfileRequest request
    ) {
        return communityProfileService.upsertMyProfile(request);
    }
}
