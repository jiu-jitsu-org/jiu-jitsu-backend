package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.CrawlFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CrawlFieldValueRepository extends JpaRepository<CrawlFieldValue, Long> {

    Optional<CrawlFieldValue> findByCompetitionIdAndFieldName(Long competitionId, String fieldName);

    List<CrawlFieldValue> findAllByCompetitionId(Long competitionId);

    /**
     * 현재 대회 칸 값과 다른 크롤러 값이 있고, 지금 값 유지(CrawlHeldValue)에 없는 대회 목록.
     * 공개 중인 대회만 (published=true).
     * 대회당 한 행씩 반환 (GROUP BY competition_id).
     */
    @Query("""
            SELECT DISTINCT cfv.competition
            FROM CrawlFieldValue cfv
            WHERE cfv.competition.published = true
              AND cfv.competition.openDate IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM CrawlHeldValue chv
                  WHERE chv.competition = cfv.competition
                    AND chv.fieldName = cfv.fieldName
                    AND chv.heldCrawlValue = cfv.crawledValue
              )
            """)
    List<com.jiujitsu.api.domain.competition.entity.Competition> findCompetitionsWithUnhandledChanges();
}
