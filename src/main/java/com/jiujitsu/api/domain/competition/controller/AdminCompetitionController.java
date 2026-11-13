package com.jiujitsu.api.domain.competition.controller;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionListItem;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionListRequest;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionRequest;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionSaveResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin-competition-controller", description = "관리자 대회 API (대회 목록·상세·만들기·고치기, 자동완성용 대회 일괄 등록)")
@CommonApiResponses
@RestController
@RequestMapping("/admin/competitions")
@RequiredArgsConstructor
public class AdminCompetitionController {

    private final CompetitionService competitionService;
    private final AdminCompetitionService adminCompetitionService;

    @Operation(
            summary = "자동완성용 대회 일괄 등록",
            description = "프로필 대회 기록의 대회명 자동완성에 쓰일 대회를 회차(대회명 + 연도) 단위로 한 번에 등록합니다 (대회일 없는 미공개). "
                    + "같은 이름이라도 연도가 다르면 따로 등록됩니다. 이미 등록됐거나 요청 안에서 겹치는 대회"
                    + "(이름 공백·대소문자 무시 + 연도 같음)와 정리하면 비거나 너무 긴 이름은 건너뛰고 skipped 에 사유와 함께 반환합니다. (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.REQUIRED_PARAMETER})
    @PostMapping("/catalog")
    public CompetitionCreateResponse createCatalog(@Valid @RequestBody CompetitionCreateRequest request) {
        return competitionService.create(request);
    }

    @Operation(
            summary = "관리자 대회 만들기",
            description = "대회명·연도와 대회 정보를 함께 넣어 대회를 만듭니다. 처음엔 미공개입니다. 대회명·연도·대회일은 필수이고 연도는 대회일 연도와 같아야 합니다. "
                    + "같은 이름(공백·대소문자 무시) + 연도의 자동완성용 대회가 있으면 새로 만들지 않고 그 대회에 정보를 채웁니다 (catalogFilled = true, 프로필 대회 기록 연결 유지). "
                    + "대회일 있는 대회와 겹치면 R0008. 연도 ≠ 대회일 연도, 일정 순서(접수 시작 ≤ 얼리버드 마감 ≤ 접수 마감 ≤ 대회일), 참가비 단계 규칙이 틀리면 R0009. (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.REQUIRED_PARAMETER, ErrorCode.WRONG_PARAMETER, ErrorCode.COMPETITION_DUPLICATED, ErrorCode.COMPETITION_INVALID_EVENT_INFO})
    @PostMapping
    public AdminCompetitionSaveResponse create(@Valid @RequestBody AdminCompetitionRequest request) {
        return adminCompetitionService.create(request);
    }

    @Operation(
            summary = "관리자 대회 고치기",
            description = "대회 정보를 전체 교체합니다 — 모든 칸을 보내야 하고, 안 보낸 칸·null 은 비워집니다 (상세 응답을 받아 고친 뒤 그대로 보내면 됨). "
                    + "공개 여부·마지막 확인일·조회수는 바뀌지 않습니다. 대회일 있는 대회만 고칠 수 있고 자동완성용 대회는 R0011 (대회 만들기로 채움). "
                    + "대회명·연도를 바꾸면 다른 대회와 겹치는지 보고(R0008), 그 대회를 고른 프로필 대회 기록의 이름·연도도 함께 바뀝니다 "
                    + "(같은 프로필에 바뀐 이름·연도로 직접 입력한 기록이 있으면 그 회원의 다음 프로필 저장이 중복으로 막히니 주의). "
                    + "연도 ≠ 대회일 연도·일정 순서·참가비 단계 규칙이 틀리면 R0009. "
                    + "공개 중인 대회의 마감을 바꾸면 바뀐 날짜 기준으로 마감 전날 알림이 갑니다. 단 매일 18시 발송 뒤에 마감을 내일로 당기면 그날 알림이 가지 않으니, "
                    + "자정 전에 POST /admin/competitions/deadline-notices 로 다시 돌려 주세요. (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND, ErrorCode.COMPETITION_CATALOG_NOT_EDITABLE, ErrorCode.REQUIRED_PARAMETER,
            ErrorCode.WRONG_PARAMETER, ErrorCode.COMPETITION_DUPLICATED, ErrorCode.COMPETITION_INVALID_EVENT_INFO})
    @PutMapping("/{competitionId}")
    public AdminCompetitionDetailResponse update(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId,
            @Valid @RequestBody AdminCompetitionRequest request) {
        return adminCompetitionService.update(competitionId, request);
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
