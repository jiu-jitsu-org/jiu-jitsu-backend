package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.CrawlRun;
import com.jiujitsu.api.domain.competition.entity.CrawlRunStatus;
import com.jiujitsu.api.domain.competition.entity.CrawlSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CrawlRunRepository extends JpaRepository<CrawlRun, Long> {

    // 사이트별 가장 최근 실행 기록
    @Query("SELECT r FROM CrawlRun r WHERE r.site = :site ORDER BY r.runAt DESC LIMIT 1")
    Optional<CrawlRun> findLatestBySite(CrawlSite site);

    // 미전달 실패 기록 목록 (failureReportedAt IS NULL AND status = FAILED)
    @Query("SELECT r FROM CrawlRun r WHERE r.status = :status AND r.failureReportedAt IS NULL ORDER BY r.runAt DESC")
    List<CrawlRun> findUnreportedByStatus(CrawlRunStatus status);

    // 특정 날짜 이후 사이트별 실행 기록 (최근 7일용)
    @Query("SELECT r FROM CrawlRun r WHERE r.runAt >= :since ORDER BY r.site, r.runAt DESC")
    List<CrawlRun> findAllSince(LocalDateTime since);
}
