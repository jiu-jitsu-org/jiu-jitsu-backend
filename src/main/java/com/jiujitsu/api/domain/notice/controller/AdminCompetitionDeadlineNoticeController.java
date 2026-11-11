package com.jiujitsu.api.domain.notice.controller;

import com.jiujitsu.api.domain.notice.dto.CompetitionDeadlineNoticeRunResponse;
import com.jiujitsu.api.domain.notice.service.CompetitionDeadlineNoticeRunService;
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

    private final CompetitionDeadlineNoticeRunService competitionDeadlineNoticeRunService;

    @Operation(
            summary = "대회 마감 전날 알림 지금 보내기",
            description = "매일 저녁 자동으로 도는 마감 전날 알림(실행일 다음 날 끝나는 얼리버드·접수 마감)을 지금 돌립니다. (ROLE_ADMIN 필요)\n\n"
                    + "- 그 시각에 서버가 내려가 있었거나 실패한 날, **같은 날 자정 전에** 부릅니다. 자정이 지나면 놓친 마감은 이미 오늘 마감이라 보내지 않고, 대신 다음 날 마감 알림을 지금(한밤중) 보냅니다\n"
                    + "- 이미 받은 유저는 건너뛰므로 여러 번 불러도 같은 알림이 다시 가지 않습니다\n"
                    + "- 응답 deadline 으로 어느 마감일을 보냈는지 확인합니다. overlapped 는 동시에 돈 다른 실행이 먼저 보낸 마감"
    )
    @PostMapping
    public CompetitionDeadlineNoticeRunResponse runTomorrow() {
        return competitionDeadlineNoticeRunService.runTomorrow();
    }
}
