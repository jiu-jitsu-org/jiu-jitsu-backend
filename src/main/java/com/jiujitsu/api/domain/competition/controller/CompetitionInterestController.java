package com.jiujitsu.api.domain.competition.controller;

import com.jiujitsu.api.domain.competition.dto.CompetitionInterestResponse;
import com.jiujitsu.api.domain.competition.service.CompetitionInterestService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.exception.annotation.LoginErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "competition-interest-controller", description = "대회 관심 API")
@CommonApiResponses
@RestController
@RequestMapping("/competitions/{competitionId}/interest")
@RequiredArgsConstructor
public class CompetitionInterestController {

    private final CompetitionInterestService competitionInterestService;

    @Operation(
            summary = "대회 관심 등록",
            description = "공개된 대회를 관심 대회로 등록합니다. 이미 등록돼 있으면 그대로 두고 같은 결과를 반환합니다. "
                    + "응답은 처리 후 내 관심 상태와 관심 수(익명)입니다."
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND})
    @PutMapping
    public CompetitionInterestResponse register(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId
    ) {
        return competitionInterestService.register(competitionId);
    }

    @Operation(
            summary = "대회 관심 해제",
            description = "관심 대회 등록을 해제합니다. 등록돼 있지 않으면 그대로 두고 같은 결과를 반환합니다. 등록 뒤 미공개로 바뀐 대회도 해제할 수 있습니다."
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND})
    @DeleteMapping
    public CompetitionInterestResponse unregister(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId
    ) {
        return competitionInterestService.unregister(competitionId);
    }
}
