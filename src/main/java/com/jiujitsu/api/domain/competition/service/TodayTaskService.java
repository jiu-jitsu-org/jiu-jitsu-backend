package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.admin.entity.Account;
import com.jiujitsu.api.domain.admin.service.AdminAuthenticationFacade;
import com.jiujitsu.api.domain.competition.dto.TodaySummaryResponse;
import com.jiujitsu.api.domain.competition.dto.TodayTaskItem;
import com.jiujitsu.api.domain.competition.dto.TodayTaskItem.*;
import com.jiujitsu.api.domain.competition.dto.TodayTaskType;
import com.jiujitsu.api.domain.competition.entity.*;
import com.jiujitsu.api.domain.competition.repository.*;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 오늘 할 일 집계 · 처리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodayTaskService {

    // 타입별 정렬 우선순위 (낮을수록 위)
    private static final Map<TodayTaskType, Integer> TYPE_ORDER = Map.of(
            TodayTaskType.CRAWL_FAILURE, 0,
            TodayTaskType.PRE_CHECK, 1,
            TodayTaskType.CHANGED_VALUE, 2,
            TodayTaskType.DISAPPEARED, 3,
            TodayTaskType.NEW_URL, 4,
            TodayTaskType.LINK_CHECK, 5,
            TodayTaskType.NEW_LINK, 6,
            TodayTaskType.INFO_REPORT, 7,
            TodayTaskType.NEW_COMPETITION, 8,
            TodayTaskType.NOTIFICATION_FAILURE, 9
    );

    private final CrawlRunRepository crawlRunRepository;
    private final NotificationPreCheckRepository preCheckRepository;
    private final RawCompetitionRepository rawCompetitionRepository;
    private final CrawlFieldValueRepository crawlFieldValueRepository;
    private final CrawlHeldValueRepository crawlHeldValueRepository;
    private final CompetitionDisappearanceRepository disappearanceRepository;
    private final StatusLinkCandidateRepository linkCandidateRepository;
    private final CompetitionLinkRepository competitionLinkRepository;
    private final ExcludedRawCompetitionRepository excludedRawCompetitionRepository;
    private final CompetitionInfoReportRepository infoReportRepository;
    private final DeadlineNotificationRepository notificationRepository;
    private final AdminAuthenticationFacade authFacade;
    private final Clock clock;

    // ── 목록 조회 ─────────────────────────────────────────────────

    public List<TodayTaskItem> getTasks(TodayTaskType filterType) {
        LocalDate today = LocalDate.now(clock);
        LocalDate tomorrow = today.plusDays(1);

        // 오늘 발송 대상 대회 ID 집합 (isPreSend 판단용)
        Set<Long> preSendCompetitionIds = notificationRepository
                .findAllByDeadlineDate(tomorrow).stream()
                .map(dn -> dn.getCompetition().getId())
                .collect(Collectors.toSet());

        List<TodayTaskItem> tasks = new ArrayList<>();

        if (filterType == null || filterType == TodayTaskType.CRAWL_FAILURE) {
            tasks.addAll(buildCrawlFailureTasks(today));
        }
        if (filterType == null || filterType == TodayTaskType.NOTIFICATION_FAILURE) {
            tasks.addAll(buildNotificationFailureTasks(today));
        }
        if (filterType == null || filterType == TodayTaskType.PRE_CHECK) {
            tasks.addAll(buildPreCheckTasks(today, preSendCompetitionIds));
        }
        if (filterType == null || filterType == TodayTaskType.NEW_COMPETITION) {
            tasks.addAll(buildNewCompetitionTasks(today));
        }
        if (filterType == null || filterType == TodayTaskType.CHANGED_VALUE) {
            tasks.addAll(buildChangedValueTasks(today, preSendCompetitionIds));
        }
        if (filterType == null || filterType == TodayTaskType.DISAPPEARED) {
            tasks.addAll(buildDisappearedTasks(today, preSendCompetitionIds));
        }
        if (filterType == null || filterType == TodayTaskType.NEW_LINK || filterType == TodayTaskType.NEW_URL) {
            tasks.addAll(buildLinkCandidateTasks(today, preSendCompetitionIds, filterType));
        }
        if (filterType == null || filterType == TodayTaskType.LINK_CHECK) {
            tasks.addAll(buildLinkCheckTasks(today, preSendCompetitionIds));
        }
        if (filterType == null || filterType == TodayTaskType.INFO_REPORT) {
            tasks.addAll(buildInfoReportTasks(today, preSendCompetitionIds));
        }

        // 정렬: CRAWL_FAILURE 먼저 → 발송 전 → 기준 날짜 오름차순 → 같은 날이면 타입 우선순위
        tasks.sort(Comparator
                .comparingInt((TodayTaskItem t) -> t.type() == TodayTaskType.CRAWL_FAILURE ? 0 : 1)
                .thenComparingInt(t -> t.preSend() ? 0 : 1)
                .thenComparingInt(t -> t.referenceDate() == null ? Integer.MAX_VALUE : (int) ChronoUnit.DAYS.between(today, t.referenceDate()))
                .thenComparingInt(t -> TYPE_ORDER.getOrDefault(t.type(), 99))
        );
        return tasks;
    }

    public TodaySummaryResponse getSummary() {
        LocalDate today = LocalDate.now(clock);
        LocalDate tomorrow = today.plusDays(1);
        LocalDateTime todayStart = today.atStartOfDay();

        // 가장 최근 실행 기록 (전체 사이트 중 최신)
        List<CrawlRun> recentRuns = crawlRunRepository.findAllSince(todayStart.minusDays(1));
        LocalDateTime lastCrawlAt = recentRuns.stream()
                .map(CrawlRun::getRunAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);

        // 사이트별 오늘 성공/실패 수
        Map<CrawlSite, CrawlRunStatus> latestStatusBySite = new LinkedHashMap<>();
        for (CrawlRun run : recentRuns) {
            latestStatusBySite.putIfAbsent(run.getSite(), run.getStatus());
        }
        long successCount = latestStatusBySite.values().stream().filter(s -> s == CrawlRunStatus.SUCCESS).count();
        long failureCount = latestStatusBySite.values().stream().filter(s -> s == CrawlRunStatus.FAILURE).count();

        long todayNotifCount = notificationRepository.countByDeadlineDateAndStatusIn(
                tomorrow, List.of(DeadlineNotificationStatus.PENDING));
        long pendingPreCheckCount = preCheckRepository.countByStatusAndDeadlineDate(
                NotificationPreCheckStatus.PENDING, tomorrow);

        // 다음 수집 시각 — 고정 06:00 (실제 설정값은 별도 API로)
        LocalDateTime nextCrawlAt = today.plusDays(1).atTime(6, 0);

        return new TodaySummaryResponse(
                lastCrawlAt,
                (int) successCount,
                (int) failureCount,
                nextCrawlAt,
                todayNotifCount,
                pendingPreCheckCount
        );
    }

    // ── 액션 처리 ─────────────────────────────────────────────────

    /**
     * 수집 실패 전달 완료
     */
    @Transactional
    public void reportCrawlFailure(Long crawlRunId) {
        Account operator = authFacade.getCurrentAccount();
        CrawlRun run = crawlRunRepository.findById(crawlRunId)
                .orElseThrow(() -> new ErrorException(ErrorCode.ENTITY_NOT_FOUND));
        run.reportFailure(operator);
    }

    /**
     * 알림 전 확인 완료
     */
    @Transactional
    public void confirmPreCheck(Long preCheckId) {
        Account operator = authFacade.getCurrentAccount();
        NotificationPreCheck check = preCheckRepository.findById(preCheckId)
                .orElseThrow(() -> new ErrorException(ErrorCode.ENTITY_NOT_FOUND));
        check.check(operator);
    }

    /**
     * 새 대회 후보 — 대회 아님 / 다른 사이트 중복으로 제외
     */
    @Transactional
    public void excludeRawCompetition(Long rawCompetitionId, ExcludedReason reason) {
        Account operator = authFacade.getCurrentAccount();
        RawCompetition raw = rawCompetitionRepository.findById(rawCompetitionId)
                .orElseThrow(() -> new ErrorException(ErrorCode.ENTITY_NOT_FOUND));
        raw.exclude(operator);
        // ExcludedRawCompetition 에 기록해 크롤러가 같은 항목을 재등록하지 않게
        if (!excludedRawCompetitionRepository.existsBySiteAndSourceUrl(raw.getSite(), raw.getSourceUrl())) {
            excludedRawCompetitionRepository.save(
                    new ExcludedRawCompetition(raw.getSite(), raw.getSourceUrl(), raw.getPageTitle(), reason, operator));
        }
    }

    /**
     * 사라짐 — 계속 공개 (확인일 오늘)
     */
    @Transactional
    public void keepPublicAfterDisappearance(Long disappearanceId) {
        Account operator = authFacade.getCurrentAccount();
        CompetitionDisappearance disappearance = disappearanceRepository.findById(disappearanceId)
                .orElseThrow(() -> new ErrorException(ErrorCode.ENTITY_NOT_FOUND));
        disappearance.resolve(DisappearanceStatus.CONTINUED, operator);
    }

    /**
     * 사라짐 — 비공개로 전환 (사유 필수)
     */
    @Transactional
    public void unpublishAfterDisappearance(Long disappearanceId) {
        Account operator = authFacade.getCurrentAccount();
        CompetitionDisappearance disappearance = disappearanceRepository.findById(disappearanceId)
                .orElseThrow(() -> new ErrorException(ErrorCode.ENTITY_NOT_FOUND));
        disappearance.resolve(DisappearanceStatus.HIDDEN, operator);
        disappearance.getCompetition().unpublish();
    }

    /**
     * 새 링크 / 새 주소 후보 승인 — 기존 링크에 주소 반영 후 승인
     */
    @Transactional
    public void approveLinkCandidate(Long candidateId) {
        Account operator = authFacade.getCurrentAccount();
        StatusLinkCandidate candidate = linkCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new ErrorException(ErrorCode.ENTITY_NOT_FOUND));
        candidate.resolve(LinkCandidateStatus.ACCEPTED, operator);
        if (candidate.getCurrentLink() == null) {
            // NEW_LINK: 새 CompetitionLink 생성 후 바로 승인
            CompetitionLink newLink = competitionLinkRepository.save(
                    new CompetitionLink(candidate.getCompetition(), candidate.getLinkType(), candidate.getCandidateUrl()));
            newLink.approve(LocalDateTime.now(clock));
        } else {
            // NEW_URL: 기존 링크 주소 교체 후 재승인
            candidate.getCurrentLink().changeUrl(candidate.getCandidateUrl());
            candidate.getCurrentLink().approve(LocalDateTime.now(clock));
        }
    }

    /**
     * 새 링크 / 새 주소 후보 거절 (같은 주소+제목 다시 안 올림)
     */
    @Transactional
    public void rejectLinkCandidate(Long candidateId) {
        Account operator = authFacade.getCurrentAccount();
        StatusLinkCandidate candidate = linkCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new ErrorException(ErrorCode.ENTITY_NOT_FOUND));
        candidate.resolve(LinkCandidateStatus.REJECTED, operator);
    }

    // ── 개별 타입 빌더 ─────────────────────────────────────────────

    private List<TodayTaskItem> buildCrawlFailureTasks(LocalDate today) {
        return crawlRunRepository.findUnreportedByStatus(CrawlRunStatus.FAILURE).stream()
                .map(run -> {
                    // 연속 실패 일수 계산 (같은 사이트의 최근 연속 실패)
                    int consecutiveDays = 1; // simplified — 실제로는 별도 쿼리 필요

                    return new TodayTaskItem(
                            run.getId(), TodayTaskType.CRAWL_FAILURE,
                            null, null, null, null, null, null, 0,
                            run.getSite().name() + " 수집 실패: " + run.getErrorType(),
                            null, null, false,
                            new CrawlFailureDetail(run.getSite(), run.getErrorType(),
                                    run.getErrorDetail(), consecutiveDays, null),
                            null, null, null, null, null, null, null
                    );
                })
                .toList();
    }

    private List<TodayTaskItem> buildNotificationFailureTasks(LocalDate today) {
        return notificationRepository.findAllByStatus(DeadlineNotificationStatus.FAILED).stream()
                .map(dn -> {
                    Competition c = dn.getCompetition();
                    return new TodayTaskItem(
                            dn.getId(), TodayTaskType.NOTIFICATION_FAILURE,
                            c.getId(), c.getName(), c.getShortName(),
                            null, c.getOpenDate(), c.getAdminStatus(), c.getInterestCount(),
                            dn.getDeadlineDate() + " 마감 알림 발송 실패",
                            referenceDate(c, today), daysLeft(c, today), false,
                            null, null, null, null, null, null, null, null
                    );
                })
                .toList();
    }

    private List<TodayTaskItem> buildPreCheckTasks(LocalDate today, Set<Long> preSendIds) {
        return preCheckRepository.findAllByStatus(NotificationPreCheckStatus.PENDING).stream()
                .map(pc -> {
                    Competition c = pc.getCompetition();
                    boolean isPreSend = preSendIds.contains(c.getId());
                    return new TodayTaskItem(
                            pc.getId(), TodayTaskType.PRE_CHECK,
                            c.getId(), c.getName(), c.getShortName(),
                            null, c.getOpenDate(), c.getAdminStatus(), c.getInterestCount(),
                            pc.getFieldName() + " 마감 내일 · 크롤러가 안 읽는 칸",
                            pc.getDeadlineDate(), daysLeftFromDate(pc.getDeadlineDate(), today), isPreSend,
                            null,
                            new PreCheckDetail(pc.getFieldName(), pc.getDeadlineDate(),
                                    null, c.getEarlyBirdDeadline(), c.getRegistrationDeadline(),
                                    c.getLastVerifiedDate()),
                            null, null, null, null, null, null
                    );
                })
                .toList();
    }

    private List<TodayTaskItem> buildNewCompetitionTasks(LocalDate today) {
        return rawCompetitionRepository.findAllByStatus(RawCompetitionStatus.PENDING).stream()
                .map(raw -> new TodayTaskItem(
                        raw.getId(), TodayTaskType.NEW_COMPETITION,
                        null, raw.getCrawledName(), null, null, null, null, 0,
                        raw.getCrawledDate() + " · " + raw.getCrawledRegion(),
                        null, null, false,
                        null, null,
                        new NewCompetitionDetail(
                                raw.getSite(), raw.getSourceUrl(), raw.getPageTitle(),
                                raw.getCrawledName(), raw.getCrawledDate(),
                                raw.getCrawledRegion(), raw.getCrawledPlace(),
                                raw.getCrawledRegistrationDeadline(),
                                raw.getCrawledRegistrationSiteUrl(),
                                raw.getFoundAt(),
                                List.of()  // 유사 대회 목록은 별도 API 에서 계산
                        ),
                        null, null, null, null, null
                ))
                .toList();
    }

    private List<TodayTaskItem> buildChangedValueTasks(LocalDate today, Set<Long> preSendIds) {
        return crawlFieldValueRepository.findCompetitionsWithUnhandledChanges().stream()
                .map(c -> {
                    List<CrawlFieldValue> allCrawled = crawlFieldValueRepository.findAllByCompetitionId(c.getId());
                    List<CrawlHeldValue> held = crawlHeldValueRepository.findAllByCompetitionId(c.getId());
                    Set<String> heldKeys = held.stream()
                            .map(h -> h.getFieldName() + "|" + h.getHeldCrawlValue())
                            .collect(Collectors.toSet());

                    List<ChangedField> changedFields = allCrawled.stream()
                            .filter(cfv -> !heldKeys.contains(cfv.getFieldName() + "|" + cfv.getCrawledValue()))
                            .map(cfv -> new ChangedField(
                                    cfv.getFieldName(), null, cfv.getCrawledValue(),
                                    isDateField(cfv.getFieldName())
                            ))
                            .toList();

                    if (changedFields.isEmpty()) return null;

                    boolean isPreSend = preSendIds.contains(c.getId());
                    String sourceUrl = allCrawled.stream().findFirst()
                            .map(cfv -> cfv.getSource() != null ? cfv.getSource().getSourceUrl() : null)
                            .orElse(null);
                    LocalDateTime crawledAt = allCrawled.stream()
                            .map(CrawlFieldValue::getCrawledAt).filter(Objects::nonNull)
                            .max(Comparator.naturalOrder()).orElse(null);

                    String summary = changedFields.size() == 1
                            ? changedFields.get(0).fieldName() + " 값 바뀜"
                            : changedFields.get(0).fieldName() + " 외 " + (changedFields.size() - 1) + "칸";

                    return new TodayTaskItem(
                            c.getId(), TodayTaskType.CHANGED_VALUE,
                            c.getId(), c.getName(), c.getShortName(),
                            null, c.getOpenDate(), c.getAdminStatus(), c.getInterestCount(),
                            summary,
                            referenceDate(c, today), daysLeft(c, today), isPreSend,
                            null, null, null,
                            new ChangedValueDetail(changedFields, crawledAt, sourceUrl),
                            null, null, null, null
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private List<TodayTaskItem> buildDisappearedTasks(LocalDate today, Set<Long> preSendIds) {
        return disappearanceRepository.findAllByStatus(DisappearanceStatus.PENDING).stream()
                .map(d -> {
                    Competition c = d.getCompetition();
                    boolean isPreSend = preSendIds.contains(c.getId());
                    return new TodayTaskItem(
                            d.getId(), TodayTaskType.DISAPPEARED,
                            c.getId(), c.getName(), c.getShortName(),
                            null, c.getOpenDate(), c.getAdminStatus(), c.getInterestCount(),
                            "원문 목록에서 사라짐 (" + d.getDisappearedAt() + ")",
                            referenceDate(c, today), daysLeft(c, today), isPreSend,
                            null, null, null, null,
                            new DisappearedDetail(d.getDisappearedAt(), d.getLastSeenAt(),
                                    c.getRegistrationDeadline(), c.getInterestCount()),
                            null, null, null
                    );
                })
                .toList();
    }

    private List<TodayTaskItem> buildLinkCandidateTasks(LocalDate today, Set<Long> preSendIds, TodayTaskType filter) {
        return linkCandidateRepository.findAllByStatus(LinkCandidateStatus.PENDING).stream()
                .filter(lc -> {
                    TodayTaskType t = lc.getCurrentLink() == null ? TodayTaskType.NEW_LINK : TodayTaskType.NEW_URL;
                    return filter == null || filter == t;
                })
                .map(lc -> {
                    Competition c = lc.getCompetition();
                    boolean isNewLink = lc.getCurrentLink() == null;
                    TodayTaskType type = isNewLink ? TodayTaskType.NEW_LINK : TodayTaskType.NEW_URL;
                    boolean isPreSend = preSendIds.contains(c.getId());
                    String summary = isNewLink
                            ? lc.getLinkType().name() + " 페이지 새로 찾음"
                            : lc.getLinkType().name() + " 새 주소 찾음";

                    return new TodayTaskItem(
                            lc.getId(), type,
                            c.getId(), c.getName(), c.getShortName(),
                            null, c.getOpenDate(), c.getAdminStatus(), c.getInterestCount(),
                            summary,
                            referenceDate(c, today), daysLeft(c, today), isPreSend,
                            null, null, null, null, null,
                            new LinkCandidateDetail(
                                    lc.getId(), lc.getLinkType(),
                                    lc.getCandidateUrl(), lc.getCandidateTitle(), lc.getFoundAt(),
                                    lc.getCurrentLink() != null ? lc.getCurrentLink().getUrl() : null,
                                    false
                            ),
                            null, null
                    );
                })
                .toList();
    }

    private List<TodayTaskItem> buildLinkCheckTasks(LocalDate today, Set<Long> preSendIds) {
        return competitionLinkRepository.findAllWithCheckNeeded().stream()
                .map(link -> {
                    Competition c = link.getCompetition();
                    boolean isPreSend = preSendIds.contains(c.getId());
                    String summary = link.getType().name() + " 링크 · " + link.getCheckNeededReason().name();

                    return new TodayTaskItem(
                            link.getId(), TodayTaskType.LINK_CHECK,
                            c.getId(), c.getName(), c.getShortName(),
                            null, c.getOpenDate(), c.getAdminStatus(), c.getInterestCount(),
                            summary,
                            referenceDate(c, today), daysLeft(c, today), isPreSend,
                            null, null, null, null, null, null,
                            new LinkCheckDetail(
                                    link.getType(), link.getCheckNeededReason(),
                                    link.getCheckNeededAt(), link.getUrl(), link.getPageTitle(),
                                    link.getLastReadAt(), c.getRegistrationDeadline()
                            ),
                            null
                    );
                })
                .toList();
    }

    private List<TodayTaskItem> buildInfoReportTasks(LocalDate today, Set<Long> preSendIds) {
        // 미처리 제보를 대회별로 묶기
        Map<Long, List<com.jiujitsu.api.domain.competition.entity.CompetitionInfoReport>> byCompetition =
                infoReportRepository.findAllUnresolved().stream()
                        .collect(Collectors.groupingBy(r -> r.getCompetition().getId()));

        return byCompetition.entrySet().stream()
                .map(entry -> {
                    var reports = entry.getValue();
                    Competition c = reports.get(0).getCompetition();
                    boolean isPreSend = preSendIds.contains(c.getId());
                    String summary = reports.size() == 1
                            ? reports.get(0).getField().name() + " 제보 1건"
                            : "제보 " + reports.size() + "건";

                    List<ReportSummary> summaries = reports.stream()
                            .map(r -> new ReportSummary(
                                    r.getId(), r.getField(), r.getDetail(), r.getCreatedAt()))
                            .toList();

                    return new TodayTaskItem(
                            reports.get(0).getId(), TodayTaskType.INFO_REPORT,
                            c.getId(), c.getName(), c.getShortName(),
                            null, c.getOpenDate(), c.getAdminStatus(), c.getInterestCount(),
                            summary,
                            referenceDate(c, today), daysLeft(c, today), isPreSend,
                            null, null, null, null, null, null, null,
                            new InfoReportDetail(summaries)
                    );
                })
                .toList();
    }

    // ── 유틸 ─────────────────────────────────────────────────────

    private LocalDate referenceDate(Competition c, LocalDate today) {
        if (c.getRegistrationDeadline() != null && !c.getRegistrationDeadline().isBefore(today)) {
            return c.getRegistrationDeadline();
        }
        return c.getOpenDate();
    }

    private Integer daysLeft(Competition c, LocalDate today) {
        LocalDate ref = referenceDate(c, today);
        return ref != null ? (int) ChronoUnit.DAYS.between(today, ref) : null;
    }

    private Integer daysLeftFromDate(LocalDate date, LocalDate today) {
        return date != null ? (int) ChronoUnit.DAYS.between(today, date) : null;
    }

    private boolean isDateField(String fieldName) {
        return fieldName != null && (
                fieldName.contains("deadline") || fieldName.contains("date") || fieldName.contains("start"));
    }
}
