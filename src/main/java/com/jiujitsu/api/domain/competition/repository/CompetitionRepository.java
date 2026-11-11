package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.Competition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
// 대회 탭 목록의 선택 필터 조합은 Specification 으로 (CompetitionSpecifications)
public interface CompetitionRepository extends JpaRepository<Competition, Long>, JpaSpecificationExecutor<Competition> {

    // 패턴은 정규화 + LIKE 이스케이프(\) 가 끝난 값 (CompetitionService 참고)
    // 정확 일치 → 앞부분 일치 → 부분 일치 순으로 먼저 보여 줘 짧은 검색어에서도 원하는 대회가 20개 안에 들게 한다
    // 연도 없는 대회는 뒤로 (NULL 정렬 순서가 H2·PG 다르므로 CASE 로 명시)
    // 자동완성은 지난 대회 기록용이라 아직 열리지 않은 대회는 뺀다
    // - 연도만 아는 카탈로그 엔트리: maxYear(올해) 이후 연도는 빼고, 연도 모르는 대회는 남긴다
    // - 개최일이 있는 대회: 오늘(당일 포함) 이후 대회는 뺀다 — 올해라도 아직 안 열린 대회가 섞이지 않게
    @Query("""
    SELECT c FROM Competition c
    WHERE c.searchName LIKE :containsPattern ESCAPE '\\'
      AND (c.competitionYear IS NULL OR c.competitionYear <= :maxYear)
      AND (c.openDate IS NULL OR c.openDate <= :today)
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
                             @Param("today") LocalDate today,
                             Pageable pageable);

    List<Competition> findAllBySearchNameIn(Collection<String> searchNames);

    // 대회 탭에 노출되는 공개 대회만 (미공개 카탈로그 엔트리는 없는 대회로 본다)
    Optional<Competition> findByIdAndPublishedTrue(Long id);

    List<Competition> findAllByIdInAndPublishedTrue(Collection<Long> ids);

    // date 에 끝나는 마감(접수 마감·얼리버드 마감·할인 단계 기한)이 있는 공개 대회 (마감 전날 알림 대상)
    @Query("""
    SELECT DISTINCT c FROM Competition c
    LEFT JOIN c.fees f
    WHERE c.published = true
      AND (c.registrationDeadline = :date OR c.earlyBirdDeadline = :date OR f.until = :date)
    """)
    List<Competition> findPublishedWithDeadlineOn(@Param("date") LocalDate date);

    // 조회수 +1 을 DB 에서 한 번에 (읽고 +1 해 저장하면 동시 요청 시 증가가 빠진다). 공개 대회만, 바뀐 행 수 반환
    // 벌크 UPDATE 라 updated_at·@PreUpdate 를 건드리지 않는다 — 조회는 대회 정보 수정이 아니다
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Competition c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id AND c.published = true")
    int incrementViewCount(@Param("id") Long id);

    @Query("SELECT c.viewCount FROM Competition c WHERE c.id = :id")
    long findViewCountById(@Param("id") Long id);
}
