package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.ChangeHistoryResponse;
import com.jiujitsu.api.domain.competition.entity.ChangeHistory;
import com.jiujitsu.api.domain.competition.repository.ChangeHistoryRepository;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대회 변경 이력 조회
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCompetitionHistoryService {

    private final ChangeHistoryRepository changeHistoryRepository;
    private final CompetitionRepository competitionRepository;

    /**
     * 대회 변경 이력 목록 — 최신순 페이징. 없는 대회면 COMPETITION_NOT_FOUND
     */
    public Page<ChangeHistoryResponse> getHistory(Long competitionId, Pageable pageable) {
        if (!competitionRepository.existsById(competitionId)) {
            throw new ErrorException(ErrorCode.COMPETITION_NOT_FOUND);
        }
        return changeHistoryRepository.findAllByCompetitionIdOrderByCreatedAtDesc(competitionId, pageable)
                .map(this::toResponse);
    }

    private ChangeHistoryResponse toResponse(ChangeHistory history) {
        String actorName = history.getActor() != null
                ? history.getActor().getName()
                : history.getActorType().name();
        return new ChangeHistoryResponse(
                history.getId(),
                history.getCreatedAt(),
                history.getActorType(),
                actorName,
                history.getAction(),
                history.getFieldName(),
                history.getOldValue(),
                history.getNewValue(),
                history.getReason(),
                history.getMemo()
        );
    }
}
