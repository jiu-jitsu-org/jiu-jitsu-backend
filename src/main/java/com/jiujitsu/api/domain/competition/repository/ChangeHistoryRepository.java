package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.ChangeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, Long> {

    Page<ChangeHistory> findAllByCompetitionIdOrderByCreatedAtDesc(Long competitionId, Pageable pageable);
}
