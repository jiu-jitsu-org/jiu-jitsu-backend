package com.jiujitsu.api.domain.competition.mapper;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionListItem;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionRequest;
import com.jiujitsu.api.domain.competition.dto.CompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionFeeResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionListItem;
import com.jiujitsu.api.domain.competition.dto.CompetitionResponse;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionEventInfo;
import com.jiujitsu.api.domain.competition.entity.CompetitionFee;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

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
    public CompetitionDetailResponse toDetail(Competition competition, LocalDate today, long interestCount, boolean interested, boolean noticeEnabled) {
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
                competition.getRegion(),
                competition.getPlace(),
                competition.getRegistrationDeadline(),
                competition.getLastVerifiedDate(),
                competition.missingRequiredFields()
        );
    }

    // 관리자 상세 — 공개 여부와 상관없이 (status 를 부르지 않는다)
    public AdminCompetitionDetailResponse toAdminDetail(Competition competition, long interestCount) {
        return new AdminCompetitionDetailResponse(
                competition.getId(),
                competition.getName(),
                competition.getCompetitionYear(),
                competition.isPublished(),
                competition.getOpenDate(),
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
                competition.missingRequiredFields()
        );
    }

    // 관리자 입력 → 대회 정보 칸. 글자 칸은 앞뒤 공백을 지우고 비면 null (빈 문자열이 "채워진 칸"으로 남지 않게), 빈 목록·null 은 빈 컬렉션
    // 컬렉션은 가변으로 만든다 (엔티티가 addAll 로 옮겨 담음)
    public CompetitionEventInfo toEventInfo(AdminCompetitionRequest request) {
        return new CompetitionEventInfo(
                request.openDate(),
                request.startTime(),
                request.region(),
                blankToNull(request.place()),
                blankToNull(request.venue()),
                blankToNull(request.host()),
                blankToNull(request.noticeUrl()),
                request.uniforms() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(request.uniforms()),
                request.audience(),
                request.registrationStartDate(),
                request.earlyBirdDeadline(),
                request.registrationDeadline(),
                request.fees() == null ? new ArrayList<>() : request.fees().stream()
                        .map(fee -> new CompetitionFee(fee.label().strip(), fee.amount(), fee.until()))
                        .collect(Collectors.toCollection(ArrayList::new)),
                blankToNull(request.feeNote())
        );
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    private static List<CompetitionFeeResponse> toFeeResponses(Competition competition) {
        return competition.getFees().stream()
                .map(fee -> new CompetitionFeeResponse(fee.getLabel(), fee.getAmount(), fee.getUntil()))
                .toList();
    }
}
