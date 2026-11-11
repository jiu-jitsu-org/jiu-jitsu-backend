package com.jiujitsu.api.domain.notice.controller;

import com.jiujitsu.api.domain.notice.dto.CompetitionDeadlineNoticeRunResponse;
import com.jiujitsu.api.domain.notice.service.CompetitionDeadlineNoticeScheduler;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin-competition-deadline-notice-controller", description = "관리자 대회 마감 알림 API")
@CommonApiResponses
@RestController
@RequestMapping("/admin/competitions/deadline-notices")
@RequiredArgsConstructor
public class AdminCompetitionDeadlineNoticeController {

    private final CompetitionDeadlineNoticeScheduler competitionDeadlineNoticeScheduler;

    @Operation(
            summary = "대회 마감 전날 알림 지금 보내기",
            description = "매일 저녁 자동으로 도는 마감 전날 알림(내일 끝나는 얼리버드·접수 마감)을 지금 돌립니다. "
                    + "그 시각에 서버가 내려가 있었거나 실패한 날 다시 보낼 때 씁니다. "
                    + "이미 받은 유저는 건너뛰므로 여러 번 불러도 같은 알림이 다시 가지 않습니다. (ROLE_ADMIN 필요)"
    )
    @PostMapping
    public CompetitionDeadlineNoticeRunResponse sendTomorrowDeadlines() {
        return competitionDeadlineNoticeScheduler.sendTomorrowDeadlines();
    }
}
