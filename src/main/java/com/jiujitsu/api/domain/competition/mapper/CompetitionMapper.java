package com.jiujitsu.api.domain.competition.mapper;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionLinkResponse;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionListItem;
import com.jiujitsu.api.domain.competition.dto.CompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionFeeResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionLinkResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionListItem;
import com.jiujitsu.api.domain.competition.dto.CompetitionResponse;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionLink;
import com.jiujitsu.api.domain.competition.entity.CompetitionLinkWindow;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class CompetitionMapper {

    public CompetitionResponse toResponse(Competition competition) {
        return new CompetitionResponse(competition.getId(), competition.getName(), competition.getCompetitionYear());
    }

    // 공개 대회만 (status 는 미공개면 예외 — 조회 쪽 published 필터 누락을 바로 드러낸다)
    public CompetitionListItem toListItem(Competition competition, LocalDate today, long interestCount, boolean interested, boolean popular) {
        return new CompetitionListItem(
                competition.getId(),
                competition.getName(),
                competition.getOpenDate(),
                competition.getEndDate(),
                competition.getRegion(),
                competition.getPlace(),
                competition.getUniforms().stream().sorted().toList(),
                competition.getAudience(),
                competition.status(today),
                competition.isEarlyBirdOpen(today),
                competition.getRegistrationStartDate(),
                competition.getEarlyBirdDeadline(),
                competition.getRegistrationDeadline(),
                competition.getViewCount(),
                interestCount,
                interested,
                popular
        );
    }

    // 공개 대회만 (toListItem 과 같음). links 는 지금 보이는 것만 거른 목록 (CompetitionQueryService#getDetail)
    public CompetitionDetailResponse toDetail(Competition competition, LocalDate today, long interestCount, boolean interested, boolean noticeEnabled,
                                              List<CompetitionLinkResponse> links) {
        return new CompetitionDetailResponse(
                today,
                competition.getId(),
                competition.getName(),
                competition.getOpenDate(),
                competition.getEndDate(),
                competition.getStartTime(),
                competition.getRegion(),
                competition.getPlace(),
                competition.getVenue(),
                competition.getUniforms().stream().sorted().toList(),
                competition.getAudience(),
                competition.getHost(),
                competition.getNoticeUrl(),
                competition.getRegistrationSite(),
                competition.isClubRegistrationOnly(),
                links,
                competition.status(today),
                competition.isEarlyBirdOpen(today),
                competition.getRegistrationStartDate(),
                competition.getEarlyBirdDeadline(),
                competition.getRegistrationDeadline(),
                toFeeResponses(competition),
                competition.getFeeNote(),
                competition.getLastVerifiedDate(),
                competition.getViewCount(),
                interestCount,
                interested,
                noticeEnabled
        );
    }

    // 관리자 목록 — 공개 여부와 상관없이 (status 를 부르지 않는다)
    public AdminCompetitionListItem toAdminListItem(Competition competition) {
        return new AdminCompetitionListItem(
                competition.getId(),
                competition.getName(),
                competition.getCompetitionYear(),
                competition.isPublished(),
                competition.getOpenDate(),
                competition.getEndDate(),
                competition.getRegion(),
                competition.getPlace(),
                competition.getRegistrationDeadline(),
                competition.getLastVerifiedDate(),
                competition.getRegistrationSite(),
                competition.missingRequiredFields()
        );
    }

    // 관리자 상세 — 공개 여부와 상관없이 (status 를 부르지 않는다)
    public AdminCompetitionDetailResponse toAdminDetail(Competition competition, long interestCount, List<AdminCompetitionLinkResponse> links) {
        return new AdminCompetitionDetailResponse(
                competition.getId(),
                competition.getName(),
                competition.getCompetitionYear(),
                competition.isPublished(),
                competition.getOpenDate(),
                competition.getEndDate(),
                competition.getStartTime(),
                competition.getRegion(),
                competition.getPlace(),
                competition.getVenue(),
                competition.getHost(),
                competition.getNoticeUrl(),
                competition.getUniforms().stream().sorted().toList(),
                competition.getAudience(),
                competition.getRegistrationStartDate(),
                competition.getEarlyBirdDeadline(),
                competition.getRegistrationDeadline(),
                toFeeResponses(competition),
                competition.getFeeNote(),
                competition.getLastVerifiedDate(),
                competition.getViewCount(),
                interestCount,
                competition.getRegistrationSite(),
                links,
                competition.missingRequiredFields()
        );
    }

    private static List<CompetitionFeeResponse> toFeeResponses(Competition competition) {
        return competition.getFees().stream()
                .map(fee -> new CompetitionFeeResponse(fee.getLabel(), fee.getAmount(), fee.getUntil()))
                .toList();
    }

    // 사용자 링크 — 보이는 기간만 (승인 상태 · 승인 시각 같은 관리용 값은 내보내지 않는다). 지금 보이는지는 호출부가 거른다
    public CompetitionLinkResponse toLink(CompetitionLink link, Competition competition) {
        CompetitionLinkWindow window = CompetitionLinkWindow.of(link, competition);
        return new CompetitionLinkResponse(link.getType(), link.getUrl(), window.from(), window.until());
    }

    // 관리자 링크 — 보이는 기간과 지금 보임 여부를 같이 (운영자가 "승인했는데 왜 안 보이지"를 화면에서 알 수 있게)
    public AdminCompetitionLinkResponse toAdminLink(CompetitionLink link, Competition competition, LocalDateTime now) {
        CompetitionLinkWindow window = CompetitionLinkWindow.of(link, competition);
        return new AdminCompetitionLinkResponse(
                link.getType(),
                link.getUrl(),
                link.getStatus(),
                link.getApprovedAt(),
                window.from(),
                window.until(),
                CompetitionLinkWindow.isVisible(link, competition, now)
        );
    }
}
