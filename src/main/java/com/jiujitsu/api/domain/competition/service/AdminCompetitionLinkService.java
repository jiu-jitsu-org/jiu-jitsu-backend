package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionLinkRequest;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionLink;
import com.jiujitsu.api.domain.competition.entity.CompetitionLinkType;
import com.jiujitsu.api.domain.competition.repository.CompetitionLinkRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.util.ConstraintViolationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 관리자 대회 상태 링크 (#221) — 넣기 · 바꾸기 · 지우기 · 승인(켜기) · 숨기기(끄기). 대회마다 종류별 하나
 * 운영자가 승인한 링크만, 보이는 기간(CompetitionLinkWindow) 안에서 앱에 나간다. 응답은 관리자 대회 상세
 * 대회일 있는 대회만 (자동완성용 대회는 COMPETITION_CATALOG_NOT_EDITABLE — AdminCompetitionService#getEditable)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCompetitionLinkService {

    private final AdminCompetitionService adminCompetitionService;
    private final CompetitionLinkRepository competitionLinkRepository;
    private final Clock clock;

    /**
     * 링크 넣기 · 주소 바꾸기. 새로 넣거나 주소가 바뀌면 승인 전 — 운영자가 다시 확인하기 전엔 보이지 않는다
     * 대한체육회 대회에 신청 링크면 COMPETITION_CLUB_REGISTRATION_ONLY
     */
    public AdminCompetitionDetailResponse put(Long competitionId, CompetitionLinkType type, CompetitionLinkRequest request) {
        Competition competition = adminCompetitionService.getEditable(competitionId);
        if (type == CompetitionLinkType.REGISTRATION && competition.isClubRegistrationOnly()) {
            throw new ErrorException(ErrorCode.COMPETITION_CLUB_REGISTRATION_ONLY);
        }
        String url = request.trimmedUrl();
        saveCheckingDuplicate(() -> competitionLinkRepository.findByCompetitionIdAndType(competitionId, type)
                .ifPresentOrElse(
                        link -> link.changeUrl(url),
                        () -> competitionLinkRepository.save(new CompetitionLink(competition, type, url))));
        return adminCompetitionService.toDetail(competition);
    }

    /**
     * 링크 지우기 (없으면 그대로)
     */
    public AdminCompetitionDetailResponse delete(Long competitionId, CompetitionLinkType type) {
        Competition competition = adminCompetitionService.getEditable(competitionId);
        competitionLinkRepository.findByCompetitionIdAndType(competitionId, type)
                .ifPresent(competitionLinkRepository::delete);
        competitionLinkRepository.flush();
        return adminCompetitionService.toDetail(competition);
    }

    /**
     * 승인(켜기). 미리 승인해도 보이는 기간 전엔 앱에 안 나간다. 없는 링크면 COMPETITION_LINK_NOT_FOUND
     */
    public AdminCompetitionDetailResponse approve(Long competitionId, CompetitionLinkType type) {
        Competition competition = adminCompetitionService.getEditable(competitionId);
        getLink(competitionId, type).approve(LocalDateTime.now(clock));
        return adminCompetitionService.toDetail(competition);
    }

    /**
     * 숨기기(끄기) — 조기 마감 · 틀린 페이지면 바로 내린다. 없는 링크면 COMPETITION_LINK_NOT_FOUND
     */
    public AdminCompetitionDetailResponse hide(Long competitionId, CompetitionLinkType type) {
        Competition competition = adminCompetitionService.getEditable(competitionId);
        getLink(competitionId, type).hide();
        return adminCompetitionService.toDetail(competition);
    }

    private CompetitionLink getLink(Long competitionId, CompetitionLinkType type) {
        return competitionLinkRepository.findByCompetitionIdAndType(competitionId, type)
                .orElseThrow(() -> new ErrorException(ErrorCode.COMPETITION_LINK_NOT_FOUND));
    }

    // 같은 대회 · 종류를 동시에 넣으면 유니크(uk_competition_link_competition_type)에 걸린다 — 다시 보내면 덮어쓰므로 잘못된 요청으로
    // 새 링크는 save(IDENTITY 라 이때 INSERT), 있는 링크는 flush 에서 터진다 — 둘 다 이 try 안
    private void saveCheckingDuplicate(Runnable write) {
        try {
            write.run();
            competitionLinkRepository.flush();
        } catch (DataIntegrityViolationException e) {
            if (!ConstraintViolationUtil.isUniqueViolation(e)) {
                throw e;
            }
            throw new ErrorException(ErrorCode.WRONG_PARAMETER);
        }
    }
}
