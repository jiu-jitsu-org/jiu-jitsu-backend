package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.ExcludedRawCompetition;
import com.jiujitsu.api.domain.competition.entity.CrawlSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExcludedRawCompetitionRepository extends JpaRepository<ExcludedRawCompetition, Long> {

    boolean existsBySiteAndSourceUrl(CrawlSite site, String sourceUrl);

    // 되살리기 대상 — restoredAt IS NULL (아직 되살리지 않은 것만)
    @Query("SELECT e FROM ExcludedRawCompetition e WHERE e.restoredAt IS NULL ORDER BY e.excludedAt DESC")
    List<ExcludedRawCompetition> findActiveExclusions();
}
