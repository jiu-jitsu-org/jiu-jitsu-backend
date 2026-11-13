package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionListItem;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionListRequest;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.mapper.CompetitionMapper;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.jiujitsu.api.domain.competition.repository.CompetitionSpecifications.*;

/**
 * 관리자 대회 관리 — 대회 탭에 나갈 대회(대회일 있는 대회)를 공개 여부와 상관없이 본다
 * 사용자 읽기 모델(CompetitionQueryService)은 공개 대회만, 자동완성 카탈로그·일괄 등록은 CompetitionService
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCompetitionService {

    private static final Sort ORDER = Sort.by(Sort.Order.asc("openDate"), Sort.Order.asc("id"));

    private final CompetitionRepository competitionRepository;
    private final CompetitionInterestService competitionInterestService;
    private final CompetitionMapper competitionMapper;

    /**
     * 관리자 대회 목록 — 대회일 있는 대회만(자동완성용 카탈로그 엔트리 제외), 대회일·id 오름차순 고정 (클라이언트 sort 는 무시)
     * from 이 to 보다 뒤면 WRONG_PARAMETER
     */
    public Page<AdminCompetitionListItem> getList(AdminCompetitionListRequest request, Pageable pageable) {
        if (request.from() != null && request.to() != null && request.from().isAfter(request.to())) {
            throw new ErrorException(ErrorCode.WRONG_PARAMETER);
        }
        Specification<Competition> filter = hasOpenDate();
        if (request.published() != null) {
            filter = filter.and(publishedIs(request.published()));
        }
        String keyword = Competition.normalize(request.keyword());
        if (!keyword.isEmpty()) {
            filter = filter.and(searchNameLike("%" + CompetitionService.escapeLike(keyword) + "%"));
        }
        if (request.from() != null) {
            filter = filter.and(openDateFrom(request.from()));
        }
        if (request.to() != null) {
            filter = filter.and(openDateTo(request.to()));
        }
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDER);
        return competitionRepository.findAll(filter, sorted).map(competitionMapper::toAdminListItem);
    }

    /**
     * 관리자 대회 상세 — 공개 여부·대회일 유무와 상관없이. 없으면 COMPETITION_NOT_FOUND
     */
    public AdminCompetitionDetailResponse getDetail(Long competitionId) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new ErrorException(ErrorCode.COMPETITION_NOT_FOUND));
        long interestCount = competitionInterestService.getInterestCounts(List.of(competitionId)).getOrDefault(competitionId, 0L);
        return competitionMapper.toAdminDetail(competition, interestCount);
    }
}
