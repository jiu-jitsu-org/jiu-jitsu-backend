package com.jiujitsu.api.domain.notice.entity;

import com.jiujitsu.api.domain.notice.dto.NoticeSettingRequest;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.global.fcm.entity.NoticeGroupType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserNoticeSettingCompetitionTest {

    private static final LocalDateTime T1 = LocalDateTime.of(2026, 11, 11, 10, 0);

    private UserNoticeSetting newSetting() {
        return new UserNoticeSetting(User.builder().id(1L).build(), false);
    }

    @Test
    @DisplayName("관심 대회 알림은 가입 때 꺼져 있고 아직 묻지 않은 상태")
    void defaults() {
        UserNoticeSetting setting = newSetting();

        assertThat(setting.isCompetitionEnabled()).isFalse();
        assertThat(setting.isCompetitionNoticeAsked()).isFalse();
        assertThat(setting.getEnabledByPushType(NoticeGroupType.COMPETITION)).isFalse();
    }

    @Test
    @DisplayName("이 칸이 생기기 전 가입자(null)는 꺼진 것으로 본다")
    void nullIsOff() {
        UserNoticeSetting setting = newSetting();
        ReflectionTestUtils.setField(setting, "competitionEnabled", null);

        assertThat(setting.getEnabledByPushType(NoticeGroupType.COMPETITION)).isFalse();
    }

    @Test
    @DisplayName("설정 저장: competitionEnabled 를 보내지 않으면(옛 앱) 그대로 두고, 보내면 바꾼다")
    void update_keepsWhenOmitted() {
        UserNoticeSetting setting = newSetting();
        setting.answerCompetitionNoticePrompt(true, T1);

        setting.update(new NoticeSettingRequest(true, true, false, false, null));
        assertThat(setting.isCompetitionEnabled()).isTrue();
        assertThat(setting.getCommunityEnabled()).isFalse();

        setting.update(new NoticeSettingRequest(true, true, true, false, false));
        assertThat(setting.isCompetitionEnabled()).isFalse();
    }

    @Test
    @DisplayName("질문 답: 알림 받기·나중에 모두 물어봤음을 남기고, 처음 물어본 시각은 바뀌지 않는다")
    void prompt() {
        UserNoticeSetting setting = newSetting();

        setting.answerCompetitionNoticePrompt(false, T1);
        assertThat(setting.isCompetitionEnabled()).isFalse();
        assertThat(setting.isCompetitionNoticeAsked()).isTrue();

        setting.answerCompetitionNoticePrompt(true, T1.plusDays(1));
        assertThat(setting.isCompetitionEnabled()).isTrue();
        assertThat(setting.getCompetitionNoticeAskedAt()).isEqualTo(T1);
        assertThat(setting.getEnabledByPushType(NoticeGroupType.COMPETITION)).isTrue();
    }
}
