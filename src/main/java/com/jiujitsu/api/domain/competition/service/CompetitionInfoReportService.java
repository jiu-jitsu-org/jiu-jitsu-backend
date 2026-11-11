package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionInfoReportResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionInfoReportRequest;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionInfoReport;
import com.jiujitsu.api.domain.competition.mapper.CompetitionInfoReportMapper;
import com.jiujitsu.api.domain.competition.repository.CompetitionInfoReportRepository;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대회 정보 제보 ("정보가 달라요") 등록과 운영자 확인
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CompetitionInfoReportService {

    static final int DAILY_LIMIT = 3;   // 같은 유저·대회·항목, 하루(서울 자정부터, ClockConfig) 최대 제보 수

    private final CompetitionInfoReportRepository competitionInfoReportRepository;
    private final CompetitionRepository competitionRepository;
    private final AuthenticationFacade authenticationFacade;
    private final CompetitionInfoReportMapper competitionInfoReportMapper;
    private final Clock clock;

    /**
     * 제보 등록. 공개 대회면 상태(종료 포함)와 상관없이 받는다
     * 유저 행을 잠가 같은 유저의 동시 요청을 차례로 처리한다 — 횟수 확인 후 저장 사이에 다른 요청이 끼지 않게
     */
    public void report(Long competitionId, CompetitionInfoReportRequest request) {
        User user = authenticationFacade.getCurrentUserForUpdate();
        Competition competition = competitionRepository.findByIdAndPublishedTrue(competitionId)
                .orElseThrow(() -> new ErrorException(ErrorCode.COMPETITION_NOT_FOUND));
        CompetitionInfoReport report = CompetitionInfoReport.of(competition, user, request.field(), request.detail());

        LocalDateTime startOfToday = LocalDate.now(clock).atStartOfDay();
        if (competitionInfoReportRepository.countSince(competitionId, user.getId(), request.field(), startOfToday) >= DAILY_LIMIT) {
            throw new ErrorException(ErrorCode.COMPETITION_INFO_REPORT_LIMIT_EXCEEDED);
        }
        competitionInfoReportRepository.save(report);
    }

    /**
     * 관리자: 제보 목록 (최신순 고정 — 클라이언트 sort 는 무시)
     */
    @Transactional(readOnly = true)
    public Page<AdminCompetitionInfoReportResponse> getAdminReports(Pageable pageable) {
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return competitionInfoReportRepository.findAllWithCompetitionAndUser(unsorted)
                .map(competitionInfoReportMapper::toAdminResponse);
    }
}
