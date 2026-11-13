package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.CompetitionInfoReport;
import org.springframework.data.jpa.domain.Specification;

/**
 * 관리자 제보 목록 조건 (#222). 처리 여부 · 대회를 골라 조합하므로 Specification 으로 (CompetitionSpecifications 와 같은 이유)
 */
public final class CompetitionInfoReportSpecifications {

    private CompetitionInfoReportSpecifications() {
    }

    // 처리 여부 (처리 = resolvedAt 있음)
    public static Specification<CompetitionInfoReport> resolvedIs(boolean resolved) {
        return (root, query, cb) -> resolved ? cb.isNotNull(root.get("resolvedAt")) : cb.isNull(root.get("resolvedAt"));
    }

    // 그 대회 제보만
    public static Specification<CompetitionInfoReport> competitionIs(Long competitionId) {
        return (root, query, cb) -> cb.equal(root.get("competition").get("id"), competitionId);
    }

    /**
     * 정렬: 미처리 먼저 → 제보 최신순 → id 역순. 처리 여부 CASE 정렬은 Sort 로 표현할 수 없어 여기에 둔다
     * Pageable 은 sort 없이 넘겨야 이 정렬이 산다 (sort 가 있으면 Spring Data 가 덮어씀). 개수 쿼리에서는 Spring Data 가 정렬을 지운다
     */
    public static Specification<CompetitionInfoReport> unresolvedFirstThenLatest() {
        return (root, query, cb) -> {
            query.orderBy(
                    cb.asc(cb.selectCase().when(cb.isNull(root.get("resolvedAt")), 0).otherwise(1)),
                    cb.desc(root.get("createdAt")),
                    cb.desc(root.get("id")));
            return null;
        };
    }
}
