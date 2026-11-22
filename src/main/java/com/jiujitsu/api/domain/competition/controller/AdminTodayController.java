package com.jiujitsu.api.domain.competition.controller;

import com.jiujitsu.api.domain.competition.dto.RawCompetitionExcludeRequest;
import com.jiujitsu.api.domain.competition.dto.TodaySummaryResponse;
import com.jiujitsu.api.domain.competition.dto.TodayTaskItem;
import com.jiujitsu.api.domain.competition.dto.TodayTaskType;
import com.jiujitsu.api.domain.competition.service.TodayTaskService;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.security.annotation.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "admin-today-controller", description = "관리자 오늘 할 일 API")
@CommonApiResponses
@AdminOnly
@RestController
@RequestMapping("/admin/today")
@RequiredArgsConstructor
public class AdminTodayController {

    private final TodayTaskService todayTaskService;

    @Operation(
            summary = "오늘 할 일 목록",
            description = "처리 안 한 할 일을 한 목록으로 반환합니다. "
                    + "정렬: 수집 실패 → 발송 전 → 기준 날짜 가까운 순 → 같은 날이면 타입 우선순위. "
                    + "type 파라미터로 종류를 거를 수 있습니다. (ROLE_ADMIN 필요)"
    )
    @GetMapping
    public List<TodayTaskItem> getTasks(
            @Parameter(description = "종류 거르기 (없으면 전체)")
            @RequestParam(required = false) TodayTaskType type) {
        return todayTaskService.getTasks(type);
    }

    @Operation(
            summary = "수집·알림 요약",
            description = "마지막 수집 시각, 성공/실패 사이트 수, 다음 수집 시각, 오늘 마감 알림 대상 수, 알림 전 확인 남은 수. (ROLE_ADMIN 필요)"
    )
    @GetMapping("/summary")
    public TodaySummaryResponse getSummary() {
        return todayTaskService.getSummary();
    }

    // ── 수집 실패 ──────────────────────────────────────────────────

    @Operation(
            summary = "수집 실패 전달 완료",
            description = "담당자에게 전달 완료 처리. 오늘 할 일 목록에서 빠집니다. (ROLE_ADMIN 필요)"
    )
    @PatchMapping("/crawl-runs/{crawlRunId}/report")
    public void reportCrawlFailure(
            @Parameter(description = "CrawlRun ID", required = true) @PathVariable Long crawlRunId) {
        todayTaskService.reportCrawlFailure(crawlRunId);
    }

    // ── 알림 전 확인 ───────────────────────────────────────────────

    @Operation(
            summary = "알림 전 확인 완료",
            description = "운영자가 대회 공지를 직접 확인 완료. 발송 예정 유지. (ROLE_ADMIN 필요)"
    )
    @PatchMapping("/pre-checks/{preCheckId}/confirm")
    public void confirmPreCheck(
            @Parameter(description = "NotificationPreCheck ID", required = true) @PathVariable Long preCheckId) {
        todayTaskService.confirmPreCheck(preCheckId);
    }

    // ── 새 대회 후보 ───────────────────────────────────────────────

    @Operation(
            summary = "새 대회 후보 — 대회 아님 / 다른 사이트 중복으로 제외",
            description = "reason: NOT_A_COMPETITION (대회 아님) | DUPLICATE_SITE (다른 사이트 중복). "
                    + "제외 목록(화면 4)에서 되살리기 가능. (ROLE_ADMIN 필요)"
    )
    @PostMapping("/raw-competitions/{rawCompetitionId}/exclude")
    public void excludeRawCompetition(
            @Parameter(description = "RawCompetition ID", required = true) @PathVariable Long rawCompetitionId,
            @Valid @RequestBody RawCompetitionExcludeRequest request) {
        todayTaskService.excludeRawCompetition(rawCompetitionId, request.reason());
    }

    // ── 원문에서 사라짐 ────────────────────────────────────────────

    @Operation(
            summary = "사라짐 — 계속 공개",
            description = "원문에서 사라진 대회를 계속 공개 상태로 유지합니다. 확인일이 오늘로 갱신됩니다. (ROLE_ADMIN 필요)"
    )
    @PatchMapping("/disappearances/{disappearanceId}/keep-public")
    public void keepPublicAfterDisappearance(
            @Parameter(description = "CompetitionDisappearance ID", required = true) @PathVariable Long disappearanceId) {
        todayTaskService.keepPublicAfterDisappearance(disappearanceId);
    }

    @Operation(
            summary = "사라짐 — 비공개로 전환",
            description = "원문에서 사라진 대회를 비공개로 내립니다. (ROLE_ADMIN 필요)"
    )
    @PatchMapping("/disappearances/{disappearanceId}/unpublish")
    public void unpublishAfterDisappearance(
            @Parameter(description = "CompetitionDisappearance ID", required = true) @PathVariable Long disappearanceId) {
        todayTaskService.unpublishAfterDisappearance(disappearanceId);
    }

    // ── 새 링크 / 새 주소 후보 ─────────────────────────────────────

    @Operation(
            summary = "링크 후보 승인",
            description = "새 링크(NEW_LINK)면 승인 전으로 저장 후 승인, 새 주소 후보(NEW_URL)면 현재 링크 주소를 교체 후 승인합니다. (ROLE_ADMIN 필요)"
    )
    @PatchMapping("/link-candidates/{candidateId}/approve")
    public void approveLinkCandidate(
            @Parameter(description = "StatusLinkCandidate ID", required = true) @PathVariable Long candidateId) {
        todayTaskService.approveLinkCandidate(candidateId);
    }

    @Operation(
            summary = "링크 후보 거절",
            description = "같은 주소+제목은 다시 올라오지 않습니다. (ROLE_ADMIN 필요)"
    )
    @PatchMapping("/link-candidates/{candidateId}/reject")
    public void rejectLinkCandidate(
            @Parameter(description = "StatusLinkCandidate ID", required = true) @PathVariable Long candidateId) {
        todayTaskService.rejectLinkCandidate(candidateId);
    }
}
