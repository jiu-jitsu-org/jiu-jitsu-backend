package com.jiujitsu.api.domain.notice.entity;

import com.jiujitsu.api.domain.notice.dto.NoticeSettingRequest;
import com.jiujitsu.api.domain.notice.mapper.NoticeMapper;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.NoticeGroupType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

// 밸런스 게임 결과 알림 설정 (#291)
class UserNoticeSettingBalanceResultTest {

    private UserNoticeSetting newSetting() {
        return new UserNoticeSetting(User.builder().id(1L).build(), false);
    }

    private static boolean resultPushEnabled(UserNoticeSetting setting) {
        return setting.getEnabledByPushType(FcmPushType.BALANCE_RESULT.getNoticeGroupType());
    }

    @Test
    @DisplayName("가입 때 켜져 있고 설정 조회에 나온다")
    void defaultOn() {
        UserNoticeSetting setting = newSetting();

        assertThat(setting.getBalanceResultEnabled()).isTrue();
        assertThat(resultPushEnabled(setting)).isTrue();
        assertThat(NoticeMapper.toNoticeSettingResponse(setting).balanceResultEnabled()).isTrue();
    }

    @Test
    @DisplayName("이 칸이 생기기 전 가입자(null)는 켜진 것으로 본다")
    void nullIsOn() {
        UserNoticeSetting setting = newSetting();
        ReflectionTestUtils.setField(setting, "balanceResultEnabled", null);

        assertThat(resultPushEnabled(setting)).isTrue();
        assertThat(NoticeMapper.toNoticeSettingResponse(setting).balanceResultEnabled()).isTrue();
    }

    @Test
    @DisplayName("설정 저장: 보내지 않으면(옛 앱) 끈 것도 켠 것도 그대로, 보내면 바꾼다")
    void update_keepsWhenOmitted() {
        UserNoticeSetting setting = newSetting();

        setting.update(new NoticeSettingRequest(true, true, true, false, null, false));
        assertThat(resultPushEnabled(setting)).isFalse();
        assertThat(NoticeMapper.toNoticeSettingResponse(setting).balanceResultEnabled()).isFalse();

        setting.update(new NoticeSettingRequest(true, true, true, false, true, null));
        assertThat(resultPushEnabled(setting)).isFalse();

        setting.update(new NoticeSettingRequest(true, true, true, false, null, true));
        setting.update(new NoticeSettingRequest(true, true, true, false, null, null));
        assertThat(resultPushEnabled(setting)).isTrue();
    }

    @Test
    @DisplayName("커뮤니티 활동 알림과 서로 영향을 주지 않는다")
    void separateFromCommunity() {
        UserNoticeSetting setting = newSetting();

        setting.update(new NoticeSettingRequest(true, true, false, false, null, null));
        assertThat(setting.getEnabledByPushType(NoticeGroupType.COMMUNITY)).isFalse();
        assertThat(resultPushEnabled(setting)).isTrue();

        setting.update(new NoticeSettingRequest(true, true, true, false, null, false));
        assertThat(setting.getEnabledByPushType(NoticeGroupType.COMMUNITY)).isTrue();
        assertThat(resultPushEnabled(setting)).isFalse();
    }
}
