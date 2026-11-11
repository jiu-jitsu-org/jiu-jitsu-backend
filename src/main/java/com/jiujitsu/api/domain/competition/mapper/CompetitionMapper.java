package com.jiujitsu.api.domain.competition.mapper;

import com.jiujitsu.api.domain.competition.dto.CompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionFeeResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionListItem;
import com.jiujitsu.api.domain.competition.dto.CompetitionResponse;
import com.jiujitsu.api.domain.competition.entity.Competition;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class CompetitionMapper {

    public CompetitionResponse toResponse(Competition competition) {
        return new CompetitionResponse(competition.getId(), competition.getName(), competition.getCompetitionYear());
    }

    // 공개 대회만 (status 는 미공개면 예외 — 조회 쪽 published 필터 누락을 바로 드러낸다)
    public CompetitionListItem toListItem(Competition competition, LocalDate today, long interestCount, boolean interested) {
        return new CompetitionListItem(
                competition.getId(),
                competition.getName(),
                competition.getOpenDate(),
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
                interested
        );
    }

    // 공개 대회만 (toListItem 과 같음)
    public CompetitionDetailResponse toDetail(Competition competition, LocalDate today, long interestCount, boolean interested) {
        return new CompetitionDetailResponse(
                today,
                competition.getId(),
                competition.getName(),
                competition.getOpenDate(),
                competition.getStartTime(),
                competition.getRegion(),
                competition.getPlace(),
                competition.getVenue(),
                competition.getUniforms().stream().sorted().toList(),
                competition.getAudience(),
                competition.getHost(),
                competition.getNoticeUrl(),
                competition.status(today),
                competition.isEarlyBirdOpen(today),
                competition.getRegistrationStartDate(),
                competition.getEarlyBirdDeadline(),
                competition.getRegistrationDeadline(),
                competition.getFees().stream()
                        .map(fee -> new CompetitionFeeResponse(fee.getLabel(), fee.getAmount(), fee.getUntil()))
                        .toList(),
                competition.getFeeNote(),
                competition.getLastVerifiedDate(),
                competition.getViewCount(),
                interestCount,
                interested
        );
    }
}
