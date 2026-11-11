package com.jiujitsu.api.domain.competition.controller;

import com.jiujitsu.api.domain.competition.dto.CompetitionNoticeResponse;
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
@RequestMapping("/competitions/{competitionId}/notice")
@RequiredArgsConstructor
public class CompetitionNoticeController {

    private final CompetitionInterestService competitionInterestService;

    @Operation(
            summary = "이 대회 알림 켜기",
            description = "이 대회의 얼리버드·접수 마감 전날 알림을 켭니다. 관심 등록하지 않은 대회면 관심도 함께 등록합니다(공개 대회만). "
                    + "이미 켜져 있으면 그대로 같은 결과를 반환합니다. 설정의 관심 대회 알림(competitionEnabled)이 꺼져 있으면 실제 알림은 오지 않습니다"
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND})
    @PutMapping
    public CompetitionNoticeResponse enable(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId
    ) {
        return competitionInterestService.enableNotice(competitionId);
    }

    @Operation(
            summary = "이 대회 알림 끄기",
            description = "이 대회의 마감 알림만 끕니다. 관심은 그대로 둡니다. 관심이 없거나 이미 꺼져 있으면 그대로 같은 결과를 반환합니다. "
                    + "미공개로 바뀐 대회도 끌 수 있습니다. 알림함의 \"이 대회 알림 받지 않기\"도 이 API (알림의 data = 대회 id)"
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND})
    @DeleteMapping
    public CompetitionNoticeResponse disable(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId
    ) {
        return competitionInterestService.disableNotice(competitionId);
    }
}
