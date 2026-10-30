package com.jiujitsu.api.domain.community.competition.repository;

import com.jiujitsu.api.domain.community.competition.entity.Competition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    // 패턴은 정규화 + LIKE 이스케이프(\) 가 끝난 값 (CompetitionService 참고)
    // 정확 일치 → 앞부분 일치 → 부분 일치 순으로 먼저 보여 줘 짧은 검색어에서도 원하는 대회가 20개 안에 들게 한다
    // 연도 없는 대회는 뒤로 (NULL 정렬 순서가 H2·PG 다르므로 CASE 로 명시)
    // 자동완성은 지난 대회 기록용이라 maxYear(올해) 이후 예정 대회는 빼고, 연도 모르는 대회는 남긴다
    @Query("""
    SELECT c FROM Competition c
    WHERE c.searchName LIKE :containsPattern ESCAPE '\\'
      AND (c.competitionYear IS NULL OR c.competitionYear <= :maxYear)
    ORDER BY CASE WHEN c.searchName = :exact THEN 0
                  WHEN c.searchName LIKE :prefixPattern ESCAPE '\\' THEN 1
                  ELSE 2 END,
             CASE WHEN c.competitionYear IS NULL THEN 1 ELSE 0 END,
             c.competitionYear DESC,
             c.name ASC,
             c.id ASC
    """)
    List<Competition> search(@Param("exact") String exact,
                             @Param("prefixPattern") String prefixPattern,
                             @Param("containsPattern") String containsPattern,
                             @Param("maxYear") int maxYear,
                             Pageable pageable);

    List<Competition> findAllBySearchNameIn(Collection<String> searchNames);
}
