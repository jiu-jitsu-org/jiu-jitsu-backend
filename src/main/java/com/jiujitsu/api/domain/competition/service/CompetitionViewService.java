package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.CompetitionViewResponse;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대회 조회수. 상세 조회(GET)와 분리한 쓰기 — 앱이 상세 화면을 열 때 한 번 부른다
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CompetitionViewService {

    private final CompetitionRepository competitionRepository;

    /**
     * 조회수 +1 후 올린 값을 돌려준다. 중복 방지 없이 부를 때마다 센다 (로그인 여부 무관, 오픈 초기 숫자가 적은 것을 고려한 결정)
     * 없거나 미공개면 COMPETITION_NOT_FOUND
     */
    public CompetitionViewResponse increase(Long competitionId) {
        if (competitionRepository.incrementViewCount(competitionId) == 0) {
            throw new ErrorException(ErrorCode.COMPETITION_NOT_FOUND);
        }
        return new CompetitionViewResponse(competitionId, competitionRepository.findViewCountById(competitionId));
    }
}
