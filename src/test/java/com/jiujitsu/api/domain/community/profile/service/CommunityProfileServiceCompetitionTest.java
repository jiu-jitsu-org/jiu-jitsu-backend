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
 * 대회 정보 저장(COMPETITION) — competitions 가 없는 요청(예: 이전 필드명 competitionInfoList)으로 기록이 지워지지 않아야 한다.
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
        profile.upsertCompetitions(List.of(new CompetitionDto(2025, "서울 오픈",
                List.of(new CompetitionDivisionDto(CompetitionRank.GOLD, null, null)))));
        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(communityProfileRepository.findByUser(user)).willReturn(Optional.of(profile));
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
}
