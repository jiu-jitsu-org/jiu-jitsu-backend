package com.jiujitsu.api.domain.user.factory;

import com.jiujitsu.api.domain.user.dto.SnsUserInfo;
import com.jiujitsu.api.domain.user.dto.UserInfo;
import com.jiujitsu.api.domain.user.entity.AccountType;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserFactoryTest {

    private final UserFactory userFactory = new UserFactory();

    private SnsUserInfo snsUserInfo(String snsId) {
        SnsUserInfo info = mock(SnsUserInfo.class);
        when(info.getSnsId()).thenReturn(snsId);
        return info;
    }

    @Test
    @DisplayName("새 회원은 무작위 분석용 ID(UUID)와 일반 계정 구분을 갖고, 회원마다 ID가 다르다")
    void createNewUser_analyticsValues() {
        User first = userFactory.createNewUser(SnsProvider.KAKAO, snsUserInfo("a"));
        User second = userFactory.createNewUser(SnsProvider.KAKAO, snsUserInfo("b"));

        assertThat(UUID.fromString(first.getAnalyticsId()).toString()).isEqualTo(first.getAnalyticsId());
        assertThat(first.getAnalyticsId()).isNotEqualTo(second.getAnalyticsId());
        assertThat(first.getAccountType()).isEqualTo(AccountType.MEMBER);
    }

    @Test
    @DisplayName("로그인 · 가입 · 토큰 갱신 응답(UserInfo)에 분석용 ID와 계정 구분을 담는다")
    void createUserInfo_analyticsValues() {
        User user = User.builder().id(3L).accountType(AccountType.INTERNAL).build();

        UserInfo info = userFactory.createUserInfo(user, false);

        assertThat(info.analyticsId()).isEqualTo(user.getAnalyticsId()).isNotBlank();
        assertThat(info.accountType()).isEqualTo(AccountType.INTERNAL);
    }
}
