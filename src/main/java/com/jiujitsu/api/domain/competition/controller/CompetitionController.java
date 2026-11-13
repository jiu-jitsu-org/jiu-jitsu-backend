package com.jiujitsu.api.domain.competition.controller;

import com.jiujitsu.api.domain.competition.dto.CompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionListRequest;
import com.jiujitsu.api.domain.competition.dto.CompetitionListResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionPickerResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionRegionsResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionViewResponse;
import com.jiujitsu.api.domain.competition.service.CompetitionQueryService;
import com.jiujitsu.api.domain.competition.service.CompetitionService;
import com.jiujitsu.api.domain.competition.service.CompetitionViewService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.exception.annotation.LoginErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "competition-controller", description = "대회 API")
@CommonApiResponses
@RestController
@RequestMapping("/competitions")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;
    private final CompetitionQueryService competitionQueryService;
    private final CompetitionViewService competitionViewService;

    @Operation(
            summary = "대회 목록 (대회 탭)",
            description = "공개 대회를 대회일 오름차순(같은 날은 id 순)으로 반환합니다. 비로그인도 조회할 수 있습니다.\n\n"
                    + "- 첫 요청: date(기준일, 기본 오늘)부터 size 개. 기준일 포함\n"
                    + "- 다음(미래 쪽): cursor=nextCursor / 이전(과거 쪽): cursor=previousCursor. 더 없으면 해당 커서가 null\n"
                    + "- cursor 를 쓸 때 필터는 같은 값으로 다시 보내고 date 는 보내지 않습니다 (함께 보내면 WRONG_PARAMETER)\n"
                    + "- 응답 목록은 방향과 상관없이 항상 오름차순입니다 (이전 페이지는 화면 위에 붙이면 됨)\n"
                    + "- 필터는 모두 AND. region·status 는 여러 개를 보내면 그중 하나. interested=true 는 로그인 필요\n"
                    + "- status·earlyBirdOpen 은 응답 today(서버 기준일) 기준입니다. 남은 날은 마감일 − today"
    )
    @ApiErrorCodeExamples({ErrorCode.WRONG_PARAMETER, ErrorCode.LOGIN_NOT_ACCESS})
    @GetMapping
    // @ParameterObject: Swagger 에 조건을 객체 하나가 아닌 쿼리 파라미터 각각으로 보여 준다
    public CompetitionListResponse getList(@Valid @ParameterObject @ModelAttribute CompetitionListRequest request) {
        return competitionQueryService.getList(request);
    }

    @Operation(
            summary = "대회 상세 (대회 탭)",
            description = "공개 대회의 상세 정보를 반환합니다. 비로그인도 조회할 수 있습니다.\n\n"
                    + "- 이 조회로는 조회수가 오르지 않습니다. 앱은 상세 화면을 열 때 POST /competitions/{competitionId}/views 를 한 번 부릅니다\n"
                    + "- status·earlyBirdOpen 은 응답 today(서버 기준일) 기준. 어느 마감을 보여 주고 강조할지는 앱 화면 규칙(날짜 원본과 today 로 계산)\n"
                    + "- fees 는 입력 순서대로, until 이 null 인 단계가 기본 참가비. 참가비가 확인되지 않았으면 빈 배열"
    )
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND})
    @GetMapping("/{competitionId}")
    public CompetitionDetailResponse getDetail(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId
    ) {
        return competitionQueryService.getDetail(competitionId);
    }

    @Operation(
            summary = "대회 조회수 올리기",
            description = "조회수를 1 올리고 올린 뒤 값을 반환합니다. 비로그인도 부를 수 있고, 부를 때마다 올라갑니다(중복 방지 없음)."
    )
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND})
    @PostMapping("/{competitionId}/views")
    public CompetitionViewResponse increaseView(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId
    ) {
        return competitionViewService.increase(competitionId);
    }

    @Operation(
            summary = "글쓰기 대회 고르기",
            description = "게시글에 연결할 공개 대회를 대회명 일부로 찾습니다. 비로그인도 조회할 수 있습니다.\n\n"
                    + "- 공백·대소문자를 무시한 부분 일치. 검색어가 비면 이름 조건 없이 같은 순서\n"
                    + "- 다가오는 대회(오늘 포함, 대회일 가까운 순) 다음에 끝난 대회(최근 순), 최대 20개\n"
                    + "- 항목은 대회 목록과 같은 카드. status·earlyBirdOpen 은 응답 today 기준\n"
                    + "- 대회 기록용 자동완성(/competitions/autocomplete)과 다릅니다 — 그쪽은 미공개 카탈로그를 포함하고 지난 대회만 줍니다"
    )
    @GetMapping("/picker")
    public CompetitionPickerResponse picker(
            @Parameter(description = "검색어 (대회명 일부, 선택)")
            @RequestParam(required = false) String keyword
    ) {
        return competitionQueryService.getPicker(keyword);
    }

    @Operation(
            summary = "대회가 있는 지역 (대회 탭 지역 필터)",
            description = "공개 대회가 하나라도 있는 지역(시·도)을 반환합니다. 비로그인도 조회할 수 있습니다.\n\n"
                    + "- 지난 대회도 포함합니다 (지난 대회만 있는 지역도 포함, 고르면 지난 대회만 나옴)\n"
                    + "- 관심·상태 등 다른 필터와 상관없이 공개 대회 전체 기준입니다\n"
                    + "- 순서는 지역 enum 선언 순서, 대회가 없으면 빈 배열"
    )
    @GetMapping("/regions")
    public CompetitionRegionsResponse regions() {
        return competitionQueryService.getRegions();
    }

    @Operation(
            summary = "등록 대회 검색 (대회명 자동완성)",
            description = "대회명 일부로 등록된 대회를 검색합니다. 공백·대소문자를 무시한 부분 일치이며, "
                    + "정확 일치 → 앞부분 일치 → 부분 일치, 그 안에서 개최 연도 최신순(연도 없는 대회는 뒤)으로 최대 20개를 반환합니다. 검색어가 비어 있으면 빈 배열을 반환합니다."
    )
    @LoginErrorExamples
    @GetMapping("/autocomplete")
    public List<CompetitionResponse> search(
            @Parameter(description = "검색어 (대회명 일부)", example = "서울")
            @RequestParam(required = false) String keyword
    ) {
        return competitionService.search(keyword);
    }
}
