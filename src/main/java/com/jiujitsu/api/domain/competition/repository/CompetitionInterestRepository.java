package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.CompetitionInterest;
import org.springframework.data.domain.Pageable;
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

    // 관심 많은 대회 (#210): from~to(한 달) 공개 대회 중 대회일이 오늘 이후(당일 포함)이고 관심이 minInterests 개 이상인 대회를
    // 관심 수 내림차순 → 대회일 빠른 순 → id 순으로. 몇 개까지는 Pageable 로 자른다
    @Query("""
    SELECT c.id
    FROM CompetitionInterest i
    JOIN i.competition c
    WHERE c.published = true
      AND c.openDate BETWEEN :from AND :to
      AND c.openDate >= :today
    GROUP BY c.id, c.openDate
    HAVING COUNT(i) >= :minInterests
    ORDER BY COUNT(i) DESC, c.openDate ASC, c.id ASC
    """)
    List<Long> findPopularIds(@Param("from") LocalDate from,
                              @Param("to") LocalDate to,
                              @Param("today") LocalDate today,
                              @Param("minInterests") long minInterests,
                              Pageable pageable);
}
