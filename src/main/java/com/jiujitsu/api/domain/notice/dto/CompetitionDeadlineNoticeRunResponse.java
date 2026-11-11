package com.jiujitsu.api.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "대회 마감 전날 알림 실행 결과")
public record CompetitionDeadlineNoticeRunResponse(
        @Schema(description = "알림을 보낸 마감일 (실행일 다음 날, 서울 기준)", example = "2026-11-20") LocalDate deadline,
        @Schema(description = "그날 끝나는 마감 수 (대회 하나의 마감 하나 = 1)", example = "3") int deadlines,
        @Schema(description = "동시에 돈 다른 실행이 먼저 보내 이번에 건너뛴 마감 수 (다시 부르면 그 사이 새로 대상이 된 유저도 받음)", example = "0") int overlapped,
        @Schema(description = "실패한 마감 수 (서버 로그 확인, 다시 부르면 재시도)", example = "0") int failed,
        @Schema(description = "이번 실행에서 새로 보낸 알림 수 (이미 받은 유저는 빠짐)", example = "12") int sent
) {
}
