package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionInterest;
import com.jiujitsu.api.domain.competition.entity.CompetitionRegion;
import com.jiujitsu.api.domain.competition.entity.CompetitionStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collection;

/**
 * 대회 탭 목록 조건. 선택 필터(지역·상태·관심·이름)를 조합하므로 @Query 하나에 `:x IS NULL OR ...` 분기를 늘리는 대신
 * 조건마다 Specification 으로 둔다 (Criteria API — 네이티브 SQL 없음, H2·PostgreSQL 같은 동작)
 */
public final class CompetitionSpecifications {

    private CompetitionSpecifications() {
    }

    // 대회 탭에는 공개 대회만 (미공개 = 자동완성용 카탈로그 엔트리 또는 공개 전 대회)
    public static Specification<Competition> published() {
        return (root, query, cb) -> cb.isTrue(root.get("published"));
    }

    // 관리자 목록: 공개 여부
    public static Specification<Competition> publishedIs(boolean published) {
        return (root, query, cb) -> cb.equal(root.get("published"), published);
    }

    // 관리자 목록: 대회일이 있는 대회만 (대회일 없는 행 = 자동완성용 카탈로그 엔트리)
    public static Specification<Competition> hasOpenDate() {
        return (root, query, cb) -> cb.isNotNull(openDate(root));
    }

    // 대회일 from 이후 (당일 포함)
    public static Specification<Competition> openDateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(openDate(root), from);
    }

    // 대회일 to 이전 (당일 포함)
    public static Specification<Competition> openDateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(openDate(root), to);
    }

    // 정렬 (openDate, id) 에서 (openDate, id) 경계보다 뒤
    public static Specification<Competition> after(LocalDate openDate, long id) {
        return (root, query, cb) -> cb.or(
                cb.greaterThan(openDate(root), openDate),
                cb.and(cb.equal(openDate(root), openDate), cb.greaterThan(root.get("id"), id)));
    }

    // 정렬 (openDate, id) 에서 (openDate, id) 경계보다 앞
    public static Specification<Competition> before(LocalDate openDate, long id) {
        return (root, query, cb) -> cb.or(
                cb.lessThan(openDate(root), openDate),
                cb.and(cb.equal(openDate(root), openDate), cb.lessThan(root.get("id"), id)));
    }

    // 끝나지 않은 대회 — 마지막 날(종료일, 하루짜리면 대회일)이 오늘 이후 (당일 포함, 진행 중인 여러 날 대회 포함). Competition#isEnded 의 반대
    public static Specification<Competition> notEnded(LocalDate today) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(lastDay(root, cb), today);
    }

    // 끝난 대회 — 마지막 날이 오늘 전. Competition#isEnded
    public static Specification<Competition> ended(LocalDate today) {
        return (root, query, cb) -> cb.lessThan(lastDay(root, cb), today);
    }

    public static Specification<Competition> regionIn(Collection<CompetitionRegion> regions) {
        return (root, query, cb) -> root.get("region").in(regions);
    }

    /**
     * 오늘 기준 상태 중 하나. Competition#status 와 같은 규칙을 날짜 조건으로 옮긴 것
     * (공개 대회는 openDate 가 항상 있다 — Competition#validateEventInfo). 둘이 어긋나지 않게 테스트로 대조한다
     */
    public static Specification<Competition> statusIn(Collection<CompetitionStatus> statuses, LocalDate today) {
        return (root, query, cb) -> cb.or(statuses.stream()
                .map(status -> statusPredicate(status, root, cb, today))
                .toArray(Predicate[]::new));
    }

    // 패턴은 정규화 + LIKE 이스케이프(\) 가 끝난 값 (자동완성 search 와 같은 규칙)
    public static Specification<Competition> searchNameLike(String pattern) {
        return (root, query, cb) -> cb.like(root.get("searchName"), pattern, '\\');
    }

    // 이 유저가 관심 등록한 대회 (조인 대신 EXISTS — 행이 늘지 않아 페이지 크기가 그대로)
    public static Specification<Competition> interestedBy(Long userId) {
        return (root, query, cb) -> {
            Subquery<Long> interest = query.subquery(Long.class);
            Root<CompetitionInterest> i = interest.from(CompetitionInterest.class);
            interest.select(i.get("id"))
                    .where(cb.equal(i.get("competition"), root), cb.equal(i.get("user").get("id"), userId));
            return cb.exists(interest);
        };
    }

    private static Predicate statusPredicate(CompetitionStatus status, Root<Competition> root, CriteriaBuilder cb, LocalDate today) {
        Path<LocalDate> openDate = openDate(root);
        Path<LocalDate> start = root.get("registrationStartDate");
        Path<LocalDate> deadline = root.get("registrationDeadline");
        Expression<LocalDate> lastDay = lastDay(root, cb);
        Predicate notEnded = cb.greaterThanOrEqualTo(lastDay, today);
        // 대회일 전·당일 — 여러 날 대회의 둘째 날부터는 접수 일정과 상관없이 CLOSED
        Predicate notStarted = cb.greaterThanOrEqualTo(openDate, today);
        Predicate notClosed = cb.or(cb.isNull(deadline), cb.greaterThanOrEqualTo(deadline, today));
        return switch (status) {
            case ENDED -> cb.lessThan(lastDay, today);
            case CLOSED -> cb.and(notEnded, cb.or(cb.lessThan(openDate, today), cb.lessThan(deadline, today)));
            case UPCOMING -> cb.and(notStarted, notClosed, cb.greaterThan(start, today));
            case OPEN -> cb.and(notStarted, notClosed, cb.or(cb.isNull(start), cb.lessThanOrEqualTo(start, today)));
        };
    }

    private static Path<LocalDate> openDate(Root<Competition> root) {
        return root.get("openDate");
    }

    // 마지막 날 = 종료일, 하루짜리(null)면 대회일 — Competition#lastDay (COALESCE: H2·PostgreSQL 같은 동작)
    private static Expression<LocalDate> lastDay(Root<Competition> root, CriteriaBuilder cb) {
        return cb.coalesce(root.<LocalDate>get("endDate"), openDate(root));
    }
}
