package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.CompetitionInfoReport;
import org.springframework.data.jpa.domain.Specification;

/**
 * 관리자 제보 목록 조건 (#222). 처리 여부 · 대회를 골라 조합하므로 Specification 으로 (CompetitionSpecifications 와 같은 이유)
 */
public final class CompetitionInfoReportSpecifications {

    private CompetitionInfoReportSpecifications() {
    }

    public static Specification<CompetitionInfoReport> resolved(boolean resolved) {
        return (root, query, cb) -> resolved ? cb.isNotNull(root.get("resolvedAt")) : cb.isNull(root.get("resolvedAt"));
    }

    public static Specification<CompetitionInfoReport> competitionIs(Long competitionId) {
        return (root, query, cb) -> cb.equal(root.get("competition").get("id"), competitionId);
    }

    /**
     * 정렬: 미처리 먼저 → 제보 최신순 → id 역순. 개수 쿼리(Long)에는 정렬을 붙이지 않는다 (PostgreSQL 은 집계 쿼리의 ORDER BY 를 거부)
     */
    public static Specification<CompetitionInfoReport> unresolvedFirstThenLatest() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(
                        cb.asc(cb.selectCase().when(cb.isNull(root.get("resolvedAt")), 0).otherwise(1)),
                        cb.desc(root.get("createdAt")),
                        cb.desc(root.get("id")));
            }
            return null;
        };
    }
}
