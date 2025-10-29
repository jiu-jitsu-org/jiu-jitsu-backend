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
@RequestMapping("/api/community/profile")
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
            summary = "내 커뮤니티 프로필 생성/수정",
            description = "현재 로그인한 사용자의 커뮤니티 프로필을 생성하거나 수정합니다."
    )
    @ApiErrorCodeExample(ErrorCode.USER_NOT_FOUND)
    @PostMapping
    public CommunityProfileResponse upsertMyProfile(
            @Valid @RequestBody CommunityProfileRequest request
    ) {
        return communityProfileService.upsertMyProfile(request);
    }

    @Operation(
            summary = "도장정보 수정",
            description = "현재 로그인한 사용자의 도장 정보 수정합니다."
    )
    @ApiErrorCodeExample(ErrorCode.USER_NOT_FOUND)
    @PostMapping("/academy")
    public CommunityProfileResponse upsertAcademyInfo(
            @Valid @RequestBody AcademyUpdateRequest request
    ) {
        return communityProfileService.upsertAcademyInfo(request);
    }

    @Operation(
            summary = "벨트/체급 정보 수정",
            description = "현재 로그인한 사용자의 벨트/체급 정보 수정합니다."
    )
    @ApiErrorCodeExample(ErrorCode.USER_NOT_FOUND)
    @PostMapping("/level")
    public CommunityProfileResponse upsertLevelInfo(
            @Valid @RequestBody LevelUpdateRequest request
    ) {
        return communityProfileService.upsertLevelInfo(request);
    }

    @Operation(
            summary = "기술 정보 수정",
            description = "현재 로그인한 사용자의 기술 정보 수정합니다."
    )
    @ApiErrorCodeExample(ErrorCode.USER_NOT_FOUND)
    @PostMapping("/technique")
    public CommunityProfileResponse upsertTechniqueInfo(
            @Valid @RequestBody TechniqueUpdateRequest request
    ) {
        return communityProfileService.upsertTechniqueInfo(request);
    }
}
