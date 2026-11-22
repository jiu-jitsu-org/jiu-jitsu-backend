package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.CompetitionSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetitionSourceRepository extends JpaRepository<CompetitionSource, Long> {

    Optional<CompetitionSource> findByCompetitionId(Long competitionId);

    boolean existsByCompetitionId(Long competitionId);
}
