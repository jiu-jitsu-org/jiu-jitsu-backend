package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.LinkCandidateStatus;
import com.jiujitsu.api.domain.competition.entity.StatusLinkCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusLinkCandidateRepository extends JpaRepository<StatusLinkCandidate, Long> {

    void deleteAllByCompetitionIdAndStatus(Long competitionId, LinkCandidateStatus status);

    java.util.List<StatusLinkCandidate> findAllByStatus(LinkCandidateStatus status);
}
