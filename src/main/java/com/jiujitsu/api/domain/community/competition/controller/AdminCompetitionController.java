package com.jiujitsu.api.domain.community.competition.controller;

import com.jiujitsu.api.domain.community.competition.dto.CompetitionCreateRequest;
import com.jiujitsu.api.domain.community.competition.dto.CompetitionCreateResponse;
import com.jiujitsu.api.domain.community.competition.service.CompetitionService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin-competition-controller", description = "관리자 등록 대회 API")
@CommonApiResponses
@RestController
@RequestMapping("/admin/competitions")
@RequiredArgsConstructor
public class AdminCompetitionController {

    private final CompetitionService competitionService;

    @Operation(
            summary = "등록 대회 일괄 등록",
            description = "대회명 자동완성에 쓰일 대회를 회차(대회명 + 연도) 단위로 한 번에 등록합니다. "
                    + "같은 이름이라도 연도가 다르면 따로 등록됩니다. 이미 등록됐거나 요청 안에서 겹치는 대회"
                    + "(이름 공백·대소문자 무시 + 연도 같음)와 정리하면 비거나 너무 긴 이름은 건너뛰고 skipped 에 사유와 함께 반환합니다. (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.REQUIRED_PARAMETER})
    @PostMapping
    public CompetitionCreateResponse create(@Valid @RequestBody CompetitionCreateRequest request) {
        return competitionService.create(request);
    }
}
