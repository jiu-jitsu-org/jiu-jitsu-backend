package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.CompetitionLink;
import com.jiujitsu.api.domain.competition.entity.CompetitionLinkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionLinkRepository extends JpaRepository<CompetitionLink, Long> {

    // 종류 선언 순서로 (enum 문자열 정렬이 아니라 서비스에서 정렬)
    List<CompetitionLink> findAllByCompetitionId(Long competitionId);

    Optional<CompetitionLink> findByCompetitionIdAndType(Long competitionId, CompetitionLinkType type);

    boolean existsByCompetitionIdAndType(Long competitionId, CompetitionLinkType type);
}
