package com.jiujitsu.api.domain.competition.controller;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionListItem;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionListRequest;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionRequest;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionSaveResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionCreateRequest;
import com.jiujitsu.api.domain.competition.dto.CompetitionLinkRequest;
import com.jiujitsu.api.domain.competition.entity.CompetitionLinkType;
import com.jiujitsu.api.domain.competition.dto.CompetitionCreateResponse;
import com.jiujitsu.api.domain.competition.service.AdminCompetitionLinkService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin-competition-controller", description = "관리자 대회 API (대회 목록·상세·만들기·고치기·확인·공개·비공개·상태 링크, 자동완성용 대회 일괄 등록)")
@CommonApiResponses
@RestController
@RequestMapping("/admin/competitions")
@RequiredArgsConstructor
public class AdminCompetitionController {

    private final CompetitionService competitionService;
    private final AdminCompetitionService adminCompetitionService;
    private final AdminCompetitionLinkService adminCompetitionLinkService;

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
                    + "대회일 있는 대회와 겹치면 R0008. 연도 ≠ 대회일 연도, 일정 순서(접수 시작 ≤ 얼리버드 마감 ≤ 접수 마감 ≤ 대회일 ≤ 종료일), 참가비 단계 규칙이 틀리면 R0009. "
                    + "여러 날 대회는 종료일(endDate, 대회일부터 14일 이내)을 넣고, 하루짜리는 비움. 접수 사이트(registrationSite)는 결과 링크 유지 · 도장 신청 안내에 씀. (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.REQUIRED_PARAMETER, ErrorCode.WRONG_PARAMETER, ErrorCode.COMPETITION_DUPLICATED, ErrorCode.COMPETITION_INVALID_EVENT_INFO})
    @PostMapping
    public AdminCompetitionSaveResponse create(@Valid @RequestBody AdminCompetitionRequest request) {
        return adminCompetitionService.create(request);
    }

    @Operation(
            summary = "관리자 대회 고치기",
            description = "대회 정보를 전체 교체합니다 — 모든 칸을 보내야 하고, 안 보낸 칸·null 은 비워집니다 (상세 응답을 받아 고친 뒤 그대로 보내면 됨). "
                    + "공개 여부·마지막 확인일·조회수는 바뀌지 않습니다 (공개 중인 대회는 필수 칸을 비워도 공개가 유지되니 missingFields 확인). 대회일 있는 대회만 고칠 수 있고 자동완성용 대회는 R0011 (대회 만들기로 채움). "
                    + "대회명·연도를 바꾸면 다른 대회와 겹치는지 보고(R0008), 그 대회를 고른 프로필 대회 기록의 이름·연도도 함께 바뀝니다 "
                    + "(같은 프로필에 바뀐 이름·연도로 직접 입력한 기록이 있으면 그 회원의 다음 프로필 저장이 중복으로 막히니 주의). "
                    + "여러 날 대회의 종료일(endDate)과 접수 사이트(registrationSite)도 같이 보내야 함 — 빼면 하루짜리 · 접수 사이트 없음(기타)으로 바뀜. "
                    + "신청 링크가 있는 대회를 대한체육회(KSOC)로 바꾸면 R0014 (신청 링크를 먼저 지움). "
                    + "연도 ≠ 대회일 연도·일정 순서(… ≤ 대회일 ≤ 종료일, 종료일은 대회일부터 14일 이내)·참가비 단계 규칙이 틀리면 R0009. "
                    + "공개 중인 대회의 마감을 바꾸면 바뀐 날짜 기준으로 마감 전날 알림이 갑니다. 단 매일 18시 발송 뒤에 마감을 내일로 당기면 그날 알림이 가지 않으니, "
                    + "자정 전에 POST /admin/competitions/deadline-notices 로 다시 돌려 주세요. (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND, ErrorCode.COMPETITION_CATALOG_NOT_EDITABLE, ErrorCode.REQUIRED_PARAMETER,
            ErrorCode.WRONG_PARAMETER, ErrorCode.COMPETITION_DUPLICATED, ErrorCode.COMPETITION_INVALID_EVENT_INFO,
            ErrorCode.COMPETITION_REGISTRATION_LINK_EXISTS})
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

    @Operation(
            summary = "관리자 대회 확인 완료",
            description = "운영자가 대회 공지와 맞춰 본 날로 마지막 확인일을 오늘로 바꿉니다. 공개·미공개 모두 됩니다. 자동완성용 대회는 R0011. (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND, ErrorCode.COMPETITION_CATALOG_NOT_EDITABLE})
    @PatchMapping("/{competitionId}/verify")
    public AdminCompetitionDetailResponse verify(@Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId) {
        return adminCompetitionService.verify(competitionId);
    }

    @Operation(
            summary = "관리자 대회 공개",
            description = "대회 탭에 공개하고 마지막 확인일을 오늘로 바꿉니다 (공개 = 공지와 맞춰 본 대회). "
                    + "대회일·시작 시각·지역·종목·접수 시작·접수 마감·대회 공지 링크 중 하나라도 비면 R0012 — 어떤 칸인지는 상세의 missingFields (빈 배열이면 공개 가능). "
                    + "이미 공개면 그대로 둡니다 (확인일 갱신은 verify). 이미 공개된 대회는 빈 칸이 있어도 공개를 유지합니다. 자동완성용 대회는 R0011. (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND, ErrorCode.COMPETITION_CATALOG_NOT_EDITABLE, ErrorCode.COMPETITION_REQUIRED_FIELDS_MISSING})
    @PatchMapping("/{competitionId}/publish")
    public AdminCompetitionDetailResponse publish(@Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId) {
        return adminCompetitionService.publish(competitionId);
    }

    @Operation(
            summary = "관리자 대회 비공개",
            description = "대회 탭에서 내립니다. 대회 목록·상세·관심 등록·마감 알림·게시글 대회 카드·대회 이야기에서 숨겨지고, 알림함의 이 대회 알림은 삭제된 대회로 보입니다. "
                    + "관심·알림 설정·게시글 연결은 지우지 않아 다시 공개하면 돌아옵니다. 단 다시 공개할 때 필수 칸을 검사하므로, 빈 칸이 있는 공개 대회를 내리면 채우기 전까지 다시 공개할 수 없습니다 (missingFields 확인). "
                    + "이미 비공개면 그대로 둡니다. 자동완성용 대회는 R0011. (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND, ErrorCode.COMPETITION_CATALOG_NOT_EDITABLE})
    @PatchMapping("/{competitionId}/unpublish")
    public AdminCompetitionDetailResponse unpublish(@Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId) {
        return adminCompetitionService.unpublish(competitionId);
    }

    @Operation(
            summary = "관리자 대회 상태 링크 넣기 · 바꾸기",
            description = "대회 상태에 맞는 접수 사이트 페이지 주소를 종류별로 하나 넣습니다 (REGISTRATION 신청, ENTRY_LIST 참가자 현황, BRACKET 대진표·시간표, LIVE_MAT 실시간 매트, RESULT 결과). "
                    + "새로 넣거나 주소가 바뀌면 승인 전(PENDING)이라 앱에 보이지 않습니다 — 원문을 확인한 뒤 approve. 같은 주소면 상태 그대로. "
                    + "대한체육회(KSOC) 대회의 신청 링크는 R0013. 응답은 관리자 대회 상세(links 포함). (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND, ErrorCode.COMPETITION_CATALOG_NOT_EDITABLE,
            ErrorCode.COMPETITION_CLUB_REGISTRATION_ONLY, ErrorCode.REQUIRED_PARAMETER, ErrorCode.WRONG_PARAMETER})
    @PutMapping("/{competitionId}/links/{type}")
    public AdminCompetitionDetailResponse putLink(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId,
            @Parameter(description = "링크 종류", required = true) @PathVariable CompetitionLinkType type,
            @Valid @RequestBody CompetitionLinkRequest request) {
        return adminCompetitionLinkService.put(competitionId, type, request);
    }

    @Operation(summary = "관리자 대회 상태 링크 지우기", description = "없으면 그대로 둡니다. (ROLE_ADMIN 필요)")
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND, ErrorCode.COMPETITION_CATALOG_NOT_EDITABLE})
    @DeleteMapping("/{competitionId}/links/{type}")
    public AdminCompetitionDetailResponse deleteLink(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId,
            @Parameter(description = "링크 종류", required = true) @PathVariable CompetitionLinkType type) {
        return adminCompetitionLinkService.delete(competitionId, type);
    }

    @Operation(
            summary = "관리자 대회 상태 링크 승인(켜기)",
            description = "원문을 확인한 링크를 켭니다. 보이는 기간(신청: 접수 시작~접수 마감 23:59 / 참가자 현황·대진표: 승인~마지막 날 23:59 / "
                    + "실시간 매트: 첫날~마지막 날 23:59 / 결과: 다음 날 00:00~14일, FLOWCOMP·주대플·대한체육회는 끝 없음, 한국 시간) 안에서만 앱에 나갑니다. "
                    + "미리 승인해 둬도 기간 전엔 안 보입니다. 처음 승인한 시각을 남깁니다. (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND, ErrorCode.COMPETITION_CATALOG_NOT_EDITABLE, ErrorCode.COMPETITION_LINK_NOT_FOUND})
    @PatchMapping("/{competitionId}/links/{type}/approve")
    public AdminCompetitionDetailResponse approveLink(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId,
            @Parameter(description = "링크 종류", required = true) @PathVariable CompetitionLinkType type) {
        return adminCompetitionLinkService.approve(competitionId, type);
    }

    @Operation(summary = "관리자 대회 상태 링크 숨기기(끄기)", description = "조기 마감 · 틀린 페이지 등으로 바로 내립니다. 다시 켜려면 approve. (ROLE_ADMIN 필요)")
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND, ErrorCode.COMPETITION_CATALOG_NOT_EDITABLE, ErrorCode.COMPETITION_LINK_NOT_FOUND})
    @PatchMapping("/{competitionId}/links/{type}/hide")
    public AdminCompetitionDetailResponse hideLink(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId,
            @Parameter(description = "링크 종류", required = true) @PathVariable CompetitionLinkType type) {
        return adminCompetitionLinkService.hide(competitionId, type);
    }
}
