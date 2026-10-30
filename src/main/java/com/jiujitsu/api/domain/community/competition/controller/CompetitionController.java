package com.jiujitsu.api.domain.community.competition.controller;

import com.jiujitsu.api.domain.community.competition.dto.CompetitionResponse;
import com.jiujitsu.api.domain.community.competition.service.CompetitionService;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.exception.annotation.LoginErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "competition-controller", description = "등록 대회 API")
@CommonApiResponses
@RestController
@RequestMapping("/community/competitions")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;

    @Operation(
            summary = "등록 대회 검색 (대회명 자동완성)",
            description = "대회명 일부로 등록된 대회를 검색합니다. 공백·대소문자를 무시한 부분 일치이며, "
                    + "정확 일치 → 앞부분 일치 → 부분 일치, 그 안에서 개최 연도 최신순(연도 없는 대회는 뒤)으로 최대 20개를 반환합니다. 검색어가 비어 있으면 빈 배열을 반환합니다."
    )
    @LoginErrorExamples
    @GetMapping
    public List<CompetitionResponse> search(
            @Parameter(description = "검색어 (대회명 일부)", example = "서울")
            @RequestParam(required = false) String keyword
    ) {
        return competitionService.search(keyword);
    }
}
