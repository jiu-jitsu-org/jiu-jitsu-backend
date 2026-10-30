package com.jiujitsu.api.domain.community.profile.service;

import com.jiujitsu.api.domain.community.competition.entity.Competition;
import com.jiujitsu.api.domain.community.competition.service.CompetitionService;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileRequest;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileResponse;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDivisionDto;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDto;
import com.jiujitsu.api.domain.community.profile.entity.*;
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

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;

/**
 * 대회 기록의 등록 대회 연결(competitionId)과 띠만 저장(BELT).
 */
@ExtendWith(MockitoExtension.class)
class CommunityProfileServiceLinkAndBeltTest {

    @Mock private CommunityProfileRepository communityProfileRepository;
    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private CompetitionService competitionService;
    @Spy private CommunityProfileMapper communityProfileMapper;

    @InjectMocks
    private CommunityProfileService communityProfileService;

    private CommunityProfile profile;

    @BeforeEach
    void setUp() {
        User user = User.builder().nickname("tester").role(UserRole.USER).build();
        profile = CommunityProfile.builder()
                .user(user)
                .beltRank(BeltRank.BLUE)
                .beltStripe(BeltStripe.STRIPE_1)
                .gender(Gender.MALE)
                .weightKg(76.5)
                .weightHidden(true)
                .competitions(new ArrayList<>())
                .build();
        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(communityProfileRepository.findByUser(user)).willReturn(Optional.of(profile));
    }

    private Competition registered(long id, String name, Integer year) {
        return Competition.builder().id(id).name(name).searchName(Competition.normalize(name)).competitionYear(year).build();
    }

    private CompetitionDto linked(int year, String name, boolean pinned, Long competitionId) {
        return new CompetitionDto(year, name, pinned, null,
                List.of(new CompetitionDivisionDto(CompetitionRank.GOLD, null, null, null, null, null)), competitionId);
    }

    private CommunityProfileRequest competitionRequest(List<CompetitionDto> competitions) {
        return new CommunityProfileRequest(ProfileRequestType.COMPETITION,
                null, null, null, null, null, competitions,
                null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("등록 대회를 고르면 대회명·연도를 등록 대회 값으로 저장하고 응답에 competitionId 를 내린다")
    void linkedCompetition_usesRegisteredNameAndYear() {
        given(competitionService.getCompetitionsByIds(Set.of(12L)))
                .willReturn(Map.of(12L, registered(12L, "서울 오픈", 2024)));

        CommunityProfileResponse response = communityProfileService.upsertMyProfile(
                competitionRequest(List.of(linked(2025, "서울오픈", true, 12L))));

        CompetitionRecord record = profile.getCompetitions().get(0);
        assertThat(record.getCompetition().getId()).isEqualTo(12L);
        assertThat(record.getCompetitionName()).isEqualTo("서울 오픈");
        assertThat(record.getCompetitionYear()).isEqualTo(2024);
        assertThat(response.competitions()).extracting(CompetitionDto::competitionId, CompetitionDto::competitionName)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(12L, "서울 오픈"));
    }

    @Test
    @DisplayName("등록 대회 연도가 없으면 보낸 연도, 대회명을 비워 보내도 등록된 이름으로 채워 고정할 수 있다")
    void linkedCompetition_withoutYear_keepsRequestYear() {
        given(competitionService.getCompetitionsByIds(Set.of(7L)))
                .willReturn(Map.of(7L, registered(7L, "Seoul Open", null)));

        communityProfileService.upsertMyProfile(competitionRequest(List.of(linked(2023, "", true, 7L))));

        CompetitionRecord record = profile.getCompetitions().get(0);
        assertThat(record.getCompetitionYear()).isEqualTo(2023);
        assertThat(record.getCompetitionName()).isEqualTo("Seoul Open");
        assertThat(record.isCompetitionPinned()).isTrue();
    }

    @Test
    @DisplayName("직접 입력한 기록만 있으면 등록 대회를 조회하지 않고 연결도 없다")
    void unlinked_doesNotQueryRegistered() {
        communityProfileService.upsertMyProfile(competitionRequest(List.of(linked(2025, "동네 대회", false, null))));

        verifyNoInteractions(competitionService);
        assertThat(profile.getCompetitions().get(0).getCompetition()).isNull();
    }

    @Test
    @DisplayName("없는 등록 대회 ID면 404(COMPETITION_NOT_FOUND)이고 저장하지 않는다")
    void unknownCompetitionId_notFound() {
        given(competitionService.getCompetitionsByIds(Set.of(99L)))
                .willThrow(new ErrorException(ErrorCode.COMPETITION_NOT_FOUND));

        assertThatThrownBy(() -> communityProfileService.upsertMyProfile(
                competitionRequest(List.of(linked(2025, "서울 오픈", false, 99L)))))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
        verify(communityProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 등록 대회를 두 번, 또는 직접 입력한 같은 이름·연도와 함께 보내면 400(COMPETITION_DUPLICATED)")
    void linkedCompetition_duplicated() {
        given(competitionService.getCompetitionsByIds(Set.of(12L)))
                .willReturn(Map.of(12L, registered(12L, "서울 오픈", 2024)));

        assertThatThrownBy(() -> communityProfileService.upsertMyProfile(competitionRequest(List.of(
                linked(2024, "아무거나", false, 12L),
                linked(2024, " 서울 오픈 ", false, null)))))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_DUPLICATED);
    }

    @Test
    @DisplayName("등록 대회 연도가 올해 이후(예정 대회)면 400(WRONG_PARAMETER)")
    void linkedCompetition_futureYear() {
        int nextYear = Year.now().getValue() + 1;
        given(competitionService.getCompetitionsByIds(Set.of(3L)))
                .willReturn(Map.of(3L, registered(3L, "내년 오픈", nextYear)));

        assertThatThrownBy(() -> communityProfileService.upsertMyProfile(
                competitionRequest(List.of(linked(2025, "내년 오픈", false, 3L)))))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
    }

    @Test
    @DisplayName("BELT 는 띠·그랄만 바꾸고 성별·체급·체급 숨김은 그대로 둔다")
    void belt_keepsWeightAndGender() {
        CommunityProfileRequest request = new CommunityProfileRequest(ProfileRequestType.BELT,
                BeltRank.PURPLE, BeltStripe.STRIPE_0, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);

        CommunityProfileResponse response = communityProfileService.upsertMyProfile(request);

        assertThat(response.beltRank()).isEqualTo(BeltRank.PURPLE);
        assertThat(response.beltStripe()).isEqualTo(BeltStripe.STRIPE_0);
        assertThat(response.gender()).isEqualTo(Gender.MALE);
        assertThat(response.weightKg()).isEqualTo(76.5);
        assertThat(response.isWeightHidden()).isTrue();
    }

    @Test
    @DisplayName("BELT 에 띠나 그랄이 없으면 400(REQUIRED_PARAMETER)이고 기존 띠는 그대로다")
    void belt_requiresRankAndStripe() {
        CommunityProfileRequest request = new CommunityProfileRequest(ProfileRequestType.BELT,
                null, BeltStripe.STRIPE_2, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> communityProfileService.upsertMyProfile(request))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REQUIRED_PARAMETER);
        assertThat(profile.getBeltRank()).isEqualTo(BeltRank.BLUE);
    }
}
