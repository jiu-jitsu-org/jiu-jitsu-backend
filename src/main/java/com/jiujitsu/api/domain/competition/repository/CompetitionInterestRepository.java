package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.CompetitionInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CompetitionInterestRepository extends JpaRepository<CompetitionInterest, Long> {

    boolean existsByCompetitionIdAndUserId(Long competitionId, Long userId);

    Optional<CompetitionInterest> findByCompetitionIdAndUserId(Long competitionId, Long userId);

    @Modifying
    @Query("DELETE FROM CompetitionInterest i WHERE i.competition.id = :competitionId AND i.user.id = :userId")
    void deleteByCompetitionIdAndUserId(@Param("competitionId") Long competitionId, @Param("userId") Long userId);

    long countByCompetitionId(Long competitionId);

    // 대회별 관심 수 (관심이 없는 대회는 행이 없다 = 0)
    @Query("""
    SELECT i.competition.id, COUNT(i)
    FROM CompetitionInterest i
    WHERE i.competition.id IN :competitionIds
    GROUP BY i.competition.id
    """)
    List<Object[]> countByCompetitionIds(@Param("competitionIds") Collection<Long> competitionIds);

    // 주어진 대회 중 이 유저가 관심 등록한 대회 id
    @Query("""
    SELECT i.competition.id
    FROM CompetitionInterest i
    WHERE i.user.id = :userId
      AND i.competition.id IN :competitionIds
    """)
    Set<Long> findInterestedCompetitionIds(@Param("userId") Long userId, @Param("competitionIds") Collection<Long> competitionIds);

    // 관심 많은 대회 후보 (#210): 대회일이 from~to 인 공개 대회 중 끝나지 않았고(마지막 날 = 종료일, 하루짜리면 대회일이 오늘 이후 — Competition#isEnded 와 같은 규칙, #223)
    // 관심이 minInterests 개 이상인 대회의 [id, 대회일]을 관심 수 내림차순 → 대회일 빠른 순 → id 순으로. 달마다 몇 개까지 자를지는 호출하는 쪽이 정한다
    // (관심 minInterests 개 이상인 대회만이라 행 수가 작다 — 여러 달을 한 번에 조회)
    @Query("""
    SELECT c.id, c.openDate
    FROM CompetitionInterest i
    JOIN i.competition c
    WHERE c.published = true
      AND c.openDate BETWEEN :from AND :to
      AND COALESCE(c.endDate, c.openDate) >= :today
    GROUP BY c.id, c.openDate
    HAVING COUNT(i) >= :minInterests
    ORDER BY COUNT(i) DESC, c.openDate ASC, c.id ASC
    """)
    List<Object[]> findPopularCandidates(@Param("from") LocalDate from,
                                         @Param("to") LocalDate to,
                                         @Param("today") LocalDate today,
                                         @Param("minInterests") long minInterests);
}
