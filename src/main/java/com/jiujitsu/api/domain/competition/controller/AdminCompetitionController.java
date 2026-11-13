package com.jiujitsu.api.domain.competition.controller;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionListItem;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionListRequest;
import com.jiujitsu.api.domain.competition.dto.CompetitionCreateRequest;
import com.jiujitsu.api.domain.competition.dto.CompetitionCreateResponse;
import com.jiujitsu.api.domain.competition.service.AdminCompetitionService;
import com.jiujitsu.api.domain.competition.service.CompetitionService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin-competition-controller", description = "관리자 대회 API (등록 대회 일괄 등록, 대회 목록·상세)")
@CommonApiResponses
@RestController
@RequestMapping("/admin/competitions")
@RequiredArgsConstructor
public class AdminCompetitionController {

    private final CompetitionService competitionService;
    private final AdminCompetitionService adminCompetitionService;

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

    @Operation(
            summary = "관리자 대회 목록",
            description = "대회일이 있는 대회를 공개·미공개 모두 페이지로 조회합니다. 대회명·연도만 있는 자동완성용 대회는 나오지 않습니다. "
                    + "대회일 오름차순(같으면 ID 순) 고정이라 sort 는 무시합니다. missingFields 는 공개에 필요한데 비어 있는 칸입니다. (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.WRONG_PARAMETER})
    @GetMapping
    public Page<AdminCompetitionListItem> getList(@ParameterObject AdminCompetitionListRequest request,
                                                  @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return adminCompetitionService.getList(request, pageable);
    }

    @Operation(
            summary = "관리자 대회 상세",
            description = "입력 화면에 채울 대회 정보 전체를 공개 여부와 상관없이 조회합니다. 조회수는 오르지 않습니다. (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND})
    @GetMapping("/{competitionId}")
    public AdminCompetitionDetailResponse getDetail(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId) {
        return adminCompetitionService.getDetail(competitionId);
    }
}
