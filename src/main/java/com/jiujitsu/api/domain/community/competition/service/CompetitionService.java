package com.jiujitsu.api.domain.community.competition.service;

import com.jiujitsu.api.domain.community.competition.dto.CompetitionCreateRequest;
import com.jiujitsu.api.domain.community.competition.dto.CompetitionCreateResponse;
import com.jiujitsu.api.domain.community.competition.dto.CompetitionCreateResponse.SkipReason;
import com.jiujitsu.api.domain.community.competition.dto.CompetitionResponse;
import com.jiujitsu.api.domain.community.competition.entity.Competition;
import com.jiujitsu.api.domain.community.competition.mapper.CompetitionMapper;
import com.jiujitsu.api.domain.community.competition.repository.CompetitionRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CompetitionService {

    static final int SEARCH_LIMIT = 20;

    private final CompetitionRepository competitionRepository;
    private final AuthenticationFacade authenticationFacade;
    private final CompetitionMapper competitionMapper;

    /**
     * 등록 대회 일괄 조회 (대회 기록 연결용). 없는 ID가 하나라도 있으면 COMPETITION_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public Map<Long, Competition> getCompetitionsByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Competition> found = competitionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Competition::getId, Function.identity()));
        if (!found.keySet().containsAll(ids)) {
            throw new ErrorException(ErrorCode.COMPETITION_NOT_FOUND);
        }
        return found;
    }

    /**
     * 등록 대회 검색 (대회명 자동완성)
     * 공백·대소문자 무시 부분 일치, 최대 20개
     */
    @Transactional(readOnly = true)
    public List<CompetitionResponse> search(String keyword) {
        authenticationFacade.checkCurrentUser();

        String normalized = Competition.normalize(keyword);
        // 저장된 이름(최대 100자)보다 긴 검색어는 일치할 수 없으므로 조회하지 않는다
        if (normalized.isEmpty() || normalized.length() > Competition.NAME_MAX_LENGTH) {
            return List.of();
        }

        String escaped = escapeLike(normalized);
        return competitionRepository.search(normalized, escaped + "%", "%" + escaped + "%", PageRequest.of(0, SEARCH_LIMIT))
                .stream()
                .map(competitionMapper::toResponse)
                .toList();
    }

    /**
     * 등록 대회 일괄 등록 (관리자)
     * 정리하면 비거나 너무 긴 이름(INVALID_NAME), 이미 등록됐거나 요청 안에서 겹치는 대회(DUPLICATE)는
     * 건너뛰고 나머지만 등록한다. 한 건 때문에 전체가 거절되면 어느 항목이 문제인지 알 수 없기 때문
     */
    public CompetitionCreateResponse create(CompetitionCreateRequest request) {
        List<Competition> candidates = new ArrayList<>();
        List<CompetitionCreateResponse.Skipped> skipped = new ArrayList<>();
        for (CompetitionCreateRequest.Item item : request.competitions()) {
            if (Competition.isRegistrableName(item.name())) {
                candidates.add(Competition.of(item.name(), item.year()));
            } else {
                skipped.add(new CompetitionCreateResponse.Skipped(item.name(), item.year(), SkipReason.INVALID_NAME));
            }
        }

        Set<String> existing = new HashSet<>();
        competitionRepository.findAllBySearchNameIn(candidates.stream().map(Competition::getSearchName).toList())
                .forEach(competition -> existing.add(competition.duplicateKey()));

        List<Competition> toSave = new ArrayList<>();
        for (Competition candidate : candidates) {
            // add 실패 = 이미 등록됐거나 이번 요청에서 먼저 나온 대회
            if (existing.add(candidate.duplicateKey())) {
                toSave.add(candidate);
            } else {
                skipped.add(new CompetitionCreateResponse.Skipped(candidate.getName(), candidate.getCompetitionYear(), SkipReason.DUPLICATE));
            }
        }

        List<CompetitionResponse> created = competitionRepository.saveAll(toSave).stream()
                .map(competitionMapper::toResponse)
                .toList();
        return new CompetitionCreateResponse(created, skipped);
    }

    // LIKE 특수문자(\ % _)를 이스케이프한다 (Repository 의 ESCAPE '\' 와 짝)
    public static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
