package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.CompetitionDisappearance;
import com.jiujitsu.api.domain.competition.entity.DisappearanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionDisappearanceRepository extends JpaRepository<CompetitionDisappearance, Long> {

    List<CompetitionDisappearance> findAllByStatus(DisappearanceStatus status);

    Optional<CompetitionDisappearance> findByCompetitionIdAndStatus(Long competitionId, DisappearanceStatus status);
}
