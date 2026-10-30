package com.jiujitsu.api.domain.community.profile.dto;

import com.jiujitsu.api.domain.community.profile.entity.BeltRank;
import com.jiujitsu.api.domain.community.profile.entity.BeltStripe;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게시글·댓글 작성자 정보에 띠·그랄·인증 여부 (#163)
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
    }
}
