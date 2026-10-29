package com.jiujitsu.api.domain.community.profile.service;

import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileRequest;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDivisionDto;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDto;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionRank;
import com.jiujitsu.api.domain.community.profile.entity.ProfileRequestType;
import com.jiujitsu.api.domain.community.profile.mapper.CommunityProfileMapper;
import com.jiujitsu.api.domain.community.profile.repository.CommunityProfileRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 대회 정보 저장(COMPETITION) — 목록 누락·중복 대회·고정 개수 검사.
 */
@ExtendWith(MockitoExtension.class)
class CommunityProfileServiceCompetitionTest {

    @Mock
    private CommunityProfileRepository communityProfileRepository;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @Spy
    private CommunityProfileMapper communityProfileMapper;

    @InjectMocks
    private CommunityProfileService communityProfileService;

    private CommunityProfile profile;

    @BeforeEach
    void setUp() {
        User user = User.builder().nickname("tester").role(UserRole.USER).build();
        profile = CommunityProfile.builder()
                .user(user)
                .competitions(new ArrayList<>())
                .build();
        profile.upsertCompetitions(List.of(competition(2025, "서울 오픈", false)));
        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(communityProfileRepository.findByUser(user)).willReturn(Optional.of(profile));
    }

    private CompetitionDto competition(int year, String name, boolean pinned) {
        return new CompetitionDto(year, name, pinned, null,
                List.of(new CompetitionDivisionDto(CompetitionRank.GOLD, null, null, null, null, null)));
    }

    private CommunityProfileRequest competitionRequest(List<CompetitionDto> competitions) {
        return new CommunityProfileRequest(ProfileRequestType.COMPETITION,
                null, null, null, null, null, competitions,
                null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("competitions 가 없으면 400(REQUIRED_PARAMETER)이고 기존 기록은 그대로다")
    void rejectMissingCompetitions() {
        assertThatThrownBy(() -> communityProfileService.upsertMyProfile(competitionRequest(null)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REQUIRED_PARAMETER);

        assertThat(profile.getCompetitions()).hasSize(1);
        verify(communityProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("빈 목록을 보내면 기록을 모두 지운다")
    void clearWithEmptyList() {
        communityProfileService.upsertMyProfile(competitionRequest(List.of()));

        assertThat(profile.getCompetitions()).isEmpty();
    }

    @Test
    @DisplayName("이름 있는 같은 대회(연도+대회명, 앞뒤 공백 무시)가 두 번 오면 R0008")
    void rejectDuplicatedCompetition() {
        assertThatThrownBy(() -> communityProfileService.upsertMyProfile(competitionRequest(List.of(
                competition(2025, "서울 오픈", false),
                competition(2025, " 서울 오픈 ", false)))))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMPETITION_DUPLICATED);
        assertThat(profile.getCompetitions()).hasSize(1);
    }

    @Test
    @DisplayName("대회명이 같아도 연도가 다르면, 대회명 없는 기록은 여러 개여도 저장된다")
    void allowDifferentYearAndNameless() {
        communityProfileService.upsertMyProfile(competitionRequest(List.of(
                competition(2025, "서울 오픈", false),
                competition(2024, "서울 오픈", false),
                competition(2024, "", false),
                competition(2024, "", false))));

        assertThat(profile.getCompetitions()).hasSize(4);
    }

    @Test
    @DisplayName("고정은 3개까지, 4개면 R0007")
    void pinLimit() {
        communityProfileService.upsertMyProfile(competitionRequest(List.of(
                competition(2025, "A", true),
                competition(2025, "B", true),
                competition(2025, "C", true),
                competition(2025, "D", false))));
        assertThat(profile.getCompetitions()).hasSize(4);

        assertThatThrownBy(() -> communityProfileService.upsertMyProfile(competitionRequest(List.of(
                competition(2025, "A", true),
                competition(2025, "B", true),
                competition(2025, "C", true),
                competition(2025, "D", true)))))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMPETITION_PIN_LIMIT_EXCEEDED);
    }
}
