package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.RawCompetition;
import com.jiujitsu.api.domain.competition.entity.RawCompetitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RawCompetitionRepository extends JpaRepository<RawCompetition, Long> {

    List<RawCompetition> findAllByStatus(RawCompetitionStatus status);

    // 입력 중(DRAFT_CREATED)인 항목 — 오늘 할 일 "입력 중" 표시용
    List<RawCompetition> findAllByLinkedCompetitionIdAndStatus(Long competitionId, RawCompetitionStatus status);
}
