package com.jiujitsu.api.domain.community.profile.service;

import com.jiujitsu.api.domain.community.profile.dto.CompetitionInfoDto;
import com.jiujitsu.api.domain.community.profile.dto.CommunityPublicProfileResponse;
import com.jiujitsu.api.domain.community.profile.entity.*;
import com.jiujitsu.api.domain.community.profile.mapper.CommunityProfileMapper;
import com.jiujitsu.api.domain.community.profile.repository.CommunityProfileRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.RecordComponent;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 다른 회원 공개 프로필 조회 (#163)
 */
@ExtendWith(MockitoExtension.class)
class CommunityProfileServicePublicProfileTest {

    @InjectMocks
    private CommunityProfileService communityProfileService;

    @Mock private CommunityProfileRepository communityProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthenticationFacade authenticationFacade;
    @Spy private CommunityProfileMapper communityProfileMapper;

    private static final Long USER_ID = 7L;

    @Test
    @DisplayName("프로필이 있으면 공개 항목(도장·띠·인증·최애·대회)을 매핑한다")
    void getPublicProfile_mapsPublicFields() {
        // given
        User user = stubUser(UserRole.USER, UserStatus.ACTIVE);
        CommunityProfile profile = profileBuilder(user)
                .competitions(new ArrayList<>(List.of(competition(2026, "서울 오픈", CompetitionRank.SILVER))))
                .build();
        given(communityProfileRepository.findByUser(user)).willReturn(Optional.of(profile));

        // when
        CommunityPublicProfileResponse response = communityProfileService.getPublicProfile(USER_ID);

        // then
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.nickname()).isEqualTo("암바장인곰");
        assertThat(response.academyName()).isEqualTo("OSS 주짓수");
        assertThat(response.beltRank()).isEqualTo(BeltRank.BLACK);
        assertThat(response.beltStripe()).isEqualTo(BeltStripe.STRIPE_2);
        assertThat(response.isOwner()).isFalse();
        assertThat(response.favoritePosition()).isEqualTo(PositionType.GUARD);
        assertThat(response.favoriteTechnique()).isEqualTo(TechniqueType.SWEEPS);
        assertThat(response.favoriteSubmission()).isEqualTo(SubmissionType.CHOKES);
        assertThat(response.competitions())
                .extracting(CompetitionInfoDto::getCompetitionName)
                .containsExactly("서울 오픈");
    }

    @Test
    @DisplayName("공개 응답에는 체급·성별·특기·지도 정보 필드가 없다")
    void publicResponse_hasNoPrivateFields() {
        Set<String> names = new HashSet<>();
        for (RecordComponent component : CommunityPublicProfileResponse.class.getRecordComponents()) {
            names.add(component.getName());
        }

        assertThat(names).doesNotContain(
                "weightKg", "isWeightHidden", "gender",
                "bestPosition", "bestTechnique", "bestSubmission",
                "teachingPhilosophy", "teachingStartDate", "teachingDetail"
        );
    }

    @Test
    @DisplayName("대회명이 null 이거나 공백인 기록은 빼고, 나머지는 저장 순서를 유지한다")
    void getPublicProfile_excludesCompetitionWithoutName() {
        // given
        User user = stubUser(UserRole.USER, UserStatus.ACTIVE);
        CommunityProfile profile = profileBuilder(user)
                .competitions(new ArrayList<>(List.of(
                        competition(2024, "부산 오픈", CompetitionRank.BRONZE),
                        competition(2025, null, CompetitionRank.GOLD),
                        competition(2025, "  ", CompetitionRank.GOLD),
                        competition(2026, "서울 오픈", CompetitionRank.SILVER)
                )))
                .build();
        given(communityProfileRepository.findByUser(user)).willReturn(Optional.of(profile));

        // when
        CommunityPublicProfileResponse response = communityProfileService.getPublicProfile(USER_ID);

        // then
        assertThat(response.competitions())
                .extracting(CompetitionInfoDto::getCompetitionName)
                .containsExactly("부산 오픈", "서울 오픈");
    }

    @Test
    @DisplayName("커뮤니티 프로필이 없으면 닉네임·사진·인증 여부만 내리고 나머지는 비운다")
    void getPublicProfile_withoutCommunityProfile() {
        // given
        User user = stubUser(UserRole.USER, UserStatus.ACTIVE);
        given(communityProfileRepository.findByUser(user)).willReturn(Optional.empty());

        // when
        CommunityPublicProfileResponse response = communityProfileService.getPublicProfile(USER_ID);

        // then
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.nickname()).isEqualTo("암바장인곰");
        assertThat(response.isOwner()).isFalse();
        assertThat(response.academyName()).isNull();
        assertThat(response.beltRank()).isNull();
        assertThat(response.beltStripe()).isNull();
        assertThat(response.favoritePosition()).isNull();
        assertThat(response.competitions()).isEmpty();
    }

    @Test
    @DisplayName("없는 회원이면 PROFILE_NOT_FOUND")
    void getPublicProfile_notFound() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> communityProfileService.getPublicProfile(USER_ID))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROFILE_NOT_FOUND);
    }

    @Test
    @DisplayName("탈퇴한 회원이면 PROFILE_NOT_FOUND")
    void getPublicProfile_deletedUser() {
        stubUser(UserRole.USER, UserStatus.DELETED);

        assertThatThrownBy(() -> communityProfileService.getPublicProfile(USER_ID))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROFILE_NOT_FOUND);
    }

    @Test
    @DisplayName("OWNER 면 isOwner=true, USER·ADMIN 은 false")
    void getPublicProfile_isOwnerByRole() {
        Map<UserRole, Boolean> expected = Map.of(
                UserRole.OWNER, true,
                UserRole.USER, false,
                UserRole.ADMIN, false
        );

        expected.forEach((role, isOwner) -> {
            User user = stubUser(role, UserStatus.ACTIVE);
            given(communityProfileRepository.findByUser(user)).willReturn(Optional.empty());

            assertThat(communityProfileService.getPublicProfile(USER_ID).isOwner())
                    .as(role.name())
                    .isEqualTo(isOwner);
        });
    }

    @Test
    @DisplayName("비로그인 허용 - 로그인 정보를 조회하지 않는다")
    void getPublicProfile_doesNotRequireLogin() {
        User user = stubUser(UserRole.USER, UserStatus.ACTIVE);
        given(communityProfileRepository.findByUser(user)).willReturn(Optional.empty());

        communityProfileService.getPublicProfile(USER_ID);

        verifyNoInteractions(authenticationFacade);
    }

    @Test
    @DisplayName("작성자 일괄 조회 - 빈 id 목록이면 쿼리 없이 빈 Map")
    void getProfilesByUserIds_emptyInput() {
        assertThat(communityProfileService.getProfilesByUserIds(List.of())).isEmpty();

        verifyNoInteractions(communityProfileRepository);
    }

    private User stubUser(UserRole role, UserStatus status) {
        User user = User.builder()
                .id(USER_ID)
                .nickname("암바장인곰")
                .role(role)
                .status(status)
                .build();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        return user;
    }

    private CommunityProfile.CommunityProfileBuilder profileBuilder(User user) {
        return CommunityProfile.builder()
                .user(user)
                .academyName("OSS 주짓수")
                .beltRank(BeltRank.BLACK)
                .beltStripe(BeltStripe.STRIPE_2)
                .gender(Gender.MALE)
                .weightKg(76.0)
                .weightHidden(false)
                .bestPosition(PositionType.TOP)
                .favoritePosition(PositionType.GUARD)
                .bestTechnique(TechniqueType.GUARD_PASSES)
                .favoriteTechnique(TechniqueType.SWEEPS)
                .bestSubmission(SubmissionType.ARM_LOCKS)
                .favoriteSubmission(SubmissionType.CHOKES);
    }

    private CompetitionInfo competition(int year, String name, CompetitionRank rank) {
        return CompetitionInfo.builder()
                .competitionYear(year)
                .competitionMonth(5)
                .competitionName(name)
                .competitionRank(rank)
                .build();
    }
}
