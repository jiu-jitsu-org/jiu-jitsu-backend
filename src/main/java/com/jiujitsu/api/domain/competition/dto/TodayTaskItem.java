package com.jiujitsu.api.domain.competition.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jiujitsu.api.domain.competition.entity.CompetitionAdminStatus;
import com.jiujitsu.api.domain.competition.entity.CompetitionInfoField;
import com.jiujitsu.api.domain.competition.entity.CompetitionLinkCheckNeededReason;
import com.jiujitsu.api.domain.competition.entity.CompetitionLinkType;
import com.jiujitsu.api.domain.competition.entity.CrawlSite;
import com.jiujitsu.api.domain.competition.entity.ExcludedReason;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 오늘 할 일 목록 한 항목.
 * typeDetail 은 type 에 따라 하나의 중첩 객체가 채워지고 나머지는 null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TodayTaskItem(

        /** 엔티티 ID (각 타입 테이블의 PK) */
        Long taskId,
        TodayTaskType type,

        // ── 대회 공통 (CRAWL_FAILURE 는 null) ───────────────────────
        Long competitionId,
        String competitionName,
        String shortName,
        String registrationSiteUrl,
        LocalDate openDate,
        CompetitionAdminStatus adminStatus,
        int interestCount,

        // ── 목록 한 줄 표시용 ─────────────────────────────────────
        /** 타입·내용을 한 줄로 요약 */
        String summary,
        /** 기준 날짜 (접수 마감, 없으면 대회일) */
        LocalDate referenceDate,
        /** 오늘 기준 남은 날 (기준 날짜 - 오늘, 음수 = 지남) */
        Integer daysLeft,
        /** 오늘 18:00 마감 알림과 관련된 항목 여부 */
        boolean preSend,

        // ── 타입별 상세 (해당 타입만 non-null) ───────────────────────
        CrawlFailureDetail crawlFailure,
        PreCheckDetail preCheck,
        NewCompetitionDetail newCompetition,
        ChangedValueDetail changedValue,
        DisappearedDetail disappeared,
        LinkCandidateDetail linkCandidate,
        LinkCheckDetail linkCheck,
        InfoReportDetail infoReport

) {

    // ── 수집 실패 ─────────────────────────────────────────────────
    public record CrawlFailureDetail(
            CrawlSite site,
            String errorType,
            String errorDetail,
            int consecutiveFailureDays,
            LocalDateTime lastSuccessAt
    ) {}

    // ── 알림 전 확인 ─────────────────────────────────────────────
    public record PreCheckDetail(
            String fieldName,
            LocalDate deadlineDate,
            String crawlerNotReadReason,  // 크롤러가 못 읽는 이유 (사이트 미지원 등)
            LocalDate earlyBirdDeadline,
            LocalDate registrationDeadline,
            LocalDate lastVerifiedDate
    ) {}

    // ── 새 대회 ───────────────────────────────────────────────────
    public record NewCompetitionDetail(
            CrawlSite site,
            String sourceUrl,
            String pageTitle,
            String crawledName,
            String crawledDate,
            String crawledRegion,
            String crawledPlace,
            String crawledRegistrationDeadline,
            String crawledRegistrationSiteUrl,
            LocalDateTime foundAt,
            List<SimilarCompetition> similarCompetitions
    ) {}

    public record SimilarCompetition(
            Long competitionId,
            String name,
            LocalDate openDate,
            String region,
            LocalDate registrationDeadline,
            boolean hasSource,         // 이미 출처가 있는 대회
            int profileRecordCount     // 연결된 프로필 기록 수 (자동완성용)
    ) {}

    // ── 바뀐 값 ──────────────────────────────────────────────────
    public record ChangedValueDetail(
            List<ChangedField> fields,
            LocalDateTime crawledAt,
            String sourceUrl
    ) {}

    public record ChangedField(
            String fieldName,
            String currentValue,
            String crawledValue,
            boolean isDateField    // 날짜 칸 여부 (알림 날짜 계산용)
    ) {}

    // ── 원문에서 사라짐 ───────────────────────────────────────────
    public record DisappearedDetail(
            LocalDate disappearedAt,
            LocalDate lastSeenAt,
            LocalDate registrationDeadline,
            int interestCount
    ) {}

    // ── 새 링크 / 새 주소 후보 ────────────────────────────────────
    public record LinkCandidateDetail(
            Long candidateId,
            CompetitionLinkType linkType,
            String candidateUrl,
            String candidateTitle,
            LocalDateTime foundAt,
            /** 새 주소 후보일 때만 — 현재 승인된 주소 */
            String currentUrl,
            /** 도메인이 접수 사이트·공지와 다를 때 경고 */
            boolean domainWarning
    ) {}

    // ── 링크 확인 ─────────────────────────────────────────────────
    public record LinkCheckDetail(
            CompetitionLinkType linkType,
            CompetitionLinkCheckNeededReason reason,
            LocalDateTime checkNeededAt,
            String linkUrl,
            String pageTitle,
            LocalDateTime lastReadAt,
            LocalDate registrationDeadline
    ) {}

    // ── 사용자 제보 ───────────────────────────────────────────────
    public record InfoReportDetail(
            List<ReportSummary> reports
    ) {}

    public record ReportSummary(
            Long reportId,
            CompetitionInfoField field,
            String detail,
            LocalDateTime createdAt
    ) {}
}
