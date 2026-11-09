package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.CompetitionInterestResponse;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionInterest;
import com.jiujitsu.api.domain.competition.repository.CompetitionInterestRepository;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CompetitionInterestService {

    private final CompetitionInterestRepository competitionInterestRepository;
    private final CompetitionRepository competitionRepository;
    private final AuthenticationFacade authenticationFacade;

    /**
     * 관심 등록. 이미 등록돼 있으면 그대로 둔다 (멱등)
     * 유저 행을 잠가 같은 유저의 동시 요청을 차례로 처리한다 — 존재 확인 후 저장 사이에 중복 저장이 끼지 않게 (유니크 제약은 최종 방어)
     */
    public CompetitionInterestResponse register(Long competitionId) {
        User user = authenticationFacade.getCurrentUserForUpdate();
        Competition competition = getPublishedCompetition(competitionId);

        if (!competitionInterestRepository.existsByCompetitionIdAndUserId(competitionId, user.getId())) {
            competitionInterestRepository.save(new CompetitionInterest(competition, user));
        }
        return new CompetitionInterestResponse(competitionId, true, competitionInterestRepository.countByCompetitionId(competitionId));
    }

    /**
     * 관심 해제. 등록돼 있지 않으면 그대로 둔다 (멱등)
     * 등록과 달리 공개 여부는 보지 않는다 — 관심 등록 뒤 미공개로 바뀐 대회도 내 관심을 지울 수 있게
     */
    public CompetitionInterestResponse unregister(Long competitionId) {
        User user = authenticationFacade.getCurrentUserForUpdate();
        if (!competitionRepository.existsById(competitionId)) {
            throw new ErrorException(ErrorCode.COMPETITION_NOT_FOUND);
        }

        competitionInterestRepository.deleteByCompetitionIdAndUserId(competitionId, user.getId());
        return new CompetitionInterestResponse(competitionId, false, competitionInterestRepository.countByCompetitionId(competitionId));
    }

    /**
     * 대회별 관심 수 일괄 조회 (목록·상세용). 관심이 없는 대회는 결과에 없다 (= 0)
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> getInterestCounts(Collection<Long> competitionIds) {
        if (competitionIds.isEmpty()) {
            return Map.of();
        }
        return competitionInterestRepository.countByCompetitionIds(competitionIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
    }

    /**
     * 주어진 대회 중 이 유저가 관심 등록한 대회 id 일괄 조회 (목록·상세의 내 관심 여부용)
     */
    @Transactional(readOnly = true)
    public Set<Long> getInterestedIds(Long userId, Collection<Long> competitionIds) {
        if (competitionIds.isEmpty()) {
            return Set.of();
        }
        return competitionInterestRepository.findInterestedCompetitionIds(userId, competitionIds);
    }

    private Competition getPublishedCompetition(Long competitionId) {
        return competitionRepository.findByIdAndPublishedTrue(competitionId)
                .orElseThrow(() -> new ErrorException(ErrorCode.COMPETITION_NOT_FOUND));
    }
}
