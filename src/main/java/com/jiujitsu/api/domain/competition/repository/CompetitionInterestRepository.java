package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.CompetitionInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
