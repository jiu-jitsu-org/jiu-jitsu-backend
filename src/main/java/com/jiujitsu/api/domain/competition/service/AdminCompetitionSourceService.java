package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.admin.entity.Account;
import com.jiujitsu.api.domain.admin.service.AdminAuthenticationFacade;
import com.jiujitsu.api.domain.competition.dto.CompetitionSourceRequest;
import com.jiujitsu.api.domain.competition.dto.CompetitionSourceResponse;
import com.jiujitsu.api.domain.competition.entity.ChangeActorType;
import com.jiujitsu.api.domain.competition.entity.ChangeHistory;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionSource;
import com.jiujitsu.api.domain.competition.repository.ChangeHistoryRepository;
import com.jiujitsu.api.domain.competition.repository.CompetitionSourceRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 대회 출처 연결 · 끊기
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCompetitionSourceService {

    private final AdminCompetitionService adminCompetitionService;
    private final CompetitionSourceRepository competitionSourceRepository;
    private final ChangeHistoryRepository changeHistoryRepository;
    private final AdminAuthenticationFacade adminAuthenticationFacade;
    private final Clock clock;

    /**
     * 출처 연결. 이미 연결된 출처가 있으면 COMPETITION_SOURCE_ALREADY_EXISTS
     */
    public CompetitionSourceResponse link(Long competitionId, CompetitionSourceRequest request) {
        Competition competition = adminCompetitionService.getEditable(competitionId);
        if (competitionSourceRepository.existsByCompetitionId(competitionId)) {
            throw new ErrorException(ErrorCode.COMPETITION_SOURCE_ALREADY_EXISTS);
        }
        Account operator = adminAuthenticationFacade.getCurrentAccount();
        LocalDateTime now = LocalDateTime.now(clock);

        CompetitionSource source = new CompetitionSource(competition, request.site(), request.sourceUrl());
        source.link(operator, now, request.pageTitle());
        competitionSourceRepository.save(source);

        changeHistoryRepository.save(ChangeHistory.builder()
                .competition(competition)
                .actorType(ChangeActorType.OPERATOR)
                .actor(operator)
                .action("source_link")
                .newValue(request.sourceUrl())
                .build());

        return toResponse(source);
    }

    /**
     * 출처 끊기. 연결된 출처가 없으면 COMPETITION_SOURCE_NOT_FOUND
     */
    public void unlink(Long competitionId) {
        Competition competition = adminCompetitionService.getEditable(competitionId);
        CompetitionSource source = competitionSourceRepository.findByCompetitionId(competitionId)
                .orElseThrow(() -> new ErrorException(ErrorCode.COMPETITION_SOURCE_NOT_FOUND));

        Account operator = adminAuthenticationFacade.getCurrentAccount();
        changeHistoryRepository.save(ChangeHistory.builder()
                .competition(competition)
                .actorType(ChangeActorType.OPERATOR)
                .actor(operator)
                .action("source_unlink")
                .oldValue(source.getSourceUrl())
                .build());

        competitionSourceRepository.delete(source);
    }

    private CompetitionSourceResponse toResponse(CompetitionSource source) {
        return new CompetitionSourceResponse(
                source.getId(),
                source.getSite(),
                source.getSourceUrl(),
                source.getPageTitle(),
                source.getLastCrawledAt(),
                source.getLastSeenInListAt()
        );
    }
}
