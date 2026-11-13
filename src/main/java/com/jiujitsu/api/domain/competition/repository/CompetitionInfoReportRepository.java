package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.CompetitionInfoField;
import com.jiujitsu.api.domain.competition.entity.CompetitionInfoReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface CompetitionInfoReportRepository extends JpaRepository<CompetitionInfoReport, Long>, JpaSpecificationExecutor<CompetitionInfoReport> {

    // 하루 횟수 제한용 — since 이후 같은 대회·유저·항목 제보 수
    @Query("""
            SELECT COUNT(r) FROM CompetitionInfoReport r
            WHERE r.competition.id = :competitionId
              AND r.user.id = :userId
              AND r.field = :field
              AND r.createdAt >= :since
            """)
    long countSince(@Param("competitionId") Long competitionId,
                    @Param("userId") Long userId,
                    @Param("field") CompetitionInfoField field,
                    @Param("since") LocalDateTime since);

    // 관리자 목록 — 조건 · 정렬은 CompetitionInfoReportSpecifications, 대회 · 제보자(ToOne)는 함께 가져온다 (N+1 없음)
    @Override
    @EntityGraph(attributePaths = {"competition", "user"})
    Page<CompetitionInfoReport> findAll(Specification<CompetitionInfoReport> spec, Pageable pageable);
}
