package com.jiujitsu.api.domain.community.profile.dto;

import com.jiujitsu.api.domain.community.profile.entity.BeltRank;
import com.jiujitsu.api.domain.community.profile.entity.BeltStripe;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.user.entity.AccountType;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게시글·댓글 작성자 정보에 띠·그랄·인증 여부 (#163), 운영자 여부 (#268)
 */
class CommunityProfileInfoTest {

    @Test
    @DisplayName("프로필이 있으면 띠·그랄을, 역할로 인증 여부를 담는다")
    void from_withProfile() {
        User owner = User.builder().id(1L).nickname("김관장").role(UserRole.OWNER).build();
        CommunityProfile profile = CommunityProfile.builder()
                .user(owner)
                .beltRank(BeltRank.BLACK)
                .beltStripe(BeltStripe.STRIPE_1)
                .build();

        CommunityProfileInfo info = CommunityProfileInfo.from(owner, profile);

        assertThat(info.id()).isEqualTo(1L);
        assertThat(info.nickname()).isEqualTo("김관장");
        assertThat(info.beltRank()).isEqualTo(BeltRank.BLACK);
        assertThat(info.beltStripe()).isEqualTo(BeltStripe.STRIPE_1);
        assertThat(info.isOwner()).isTrue();
        assertThat(info.isOperator()).isFalse();
    }

    @Test
    @DisplayName("프로필이 없으면 띠·그랄은 null, 인증 여부는 역할 기준")
    void from_withoutProfile() {
        User user = User.builder().id(2L).nickname("새벽반흰띠").role(UserRole.USER).build();

        CommunityProfileInfo info = CommunityProfileInfo.from(user, null);

        assertThat(info.id()).isEqualTo(2L);
        assertThat(info.beltRank()).isNull();
        assertThat(info.beltStripe()).isNull();
        assertThat(info.isOwner()).isFalse();
        assertThat(info.isOperator()).isFalse();
    }

    @Test
    @DisplayName("계정 구분이 OPERATOR 면 운영자 (역할과 무관), 닉네임은 그대로 (#268)")
    void from_operatorAccount() {
        User operator = User.builder().id(3L).nickname("운영자의 닉네임").role(UserRole.USER).accountType(AccountType.OPERATOR).build();

        CommunityProfileInfo info = CommunityProfileInfo.from(operator, null);

        assertThat(info.isOperator()).isTrue();
        assertThat(info.isOwner()).isFalse();
        assertThat(info.nickname()).isEqualTo("운영자의 닉네임");
    }

    @Test
    @DisplayName("관리자 역할(ADMIN)이나 팀 계정(INTERNAL)만으로는 운영자 아님 (#268)")
    void adminRoleOrInternal_notOperator() {
        User admin = User.builder().id(4L).role(UserRole.ADMIN).build();
        User internal = User.builder().id(5L).role(UserRole.USER).accountType(AccountType.INTERNAL).build();

        assertThat(CommunityProfileInfo.from(admin, null).isOperator()).isFalse();
        assertThat(CommunityProfileInfo.from(internal, null).isOperator()).isFalse();
    }

    @Test
    @DisplayName("탈퇴 작성자면 운영자 여부도 null (#268)")
    void withdrawn() {
        assertThat(CommunityProfileInfo.from(null, null).isOperator()).isNull();
    }

    @Test
    @DisplayName("마스킹(차단 작성자)이면 띠·그랄·인증 여부도 모두 null")
    void masked() {
        CommunityProfileInfo info = CommunityProfileInfo.masked();

        assertThat(info.id()).isNull();
        assertThat(info.nickname()).isNull();
        assertThat(info.profileImage()).isNull();
        assertThat(info.beltRank()).isNull();
        assertThat(info.beltStripe()).isNull();
        assertThat(info.isOwner()).isNull();
        assertThat(info.isOperator()).isNull();
    }
}
