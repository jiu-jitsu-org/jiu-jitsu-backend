package com.jiujitsu.api.global.fcm.event;

import com.jiujitsu.api.domain.notice.dto.NoticeSettingRequest;
import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.service.FcmPushService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

// 결과 알림 푸시는 결과 알림 설정만 본다 (#291). 알림함 저장은 발송 지점에서 이미 끝났다
@ExtendWith(MockitoExtension.class)
class FcmPushEventListenerBalanceResultTest {

    private static final Map<String, String> DATA = Map.of("type", "BALANCE_DETAIL", "data", "100");
    private static final FcmPushRequestedEvent EVENT =
            new FcmPushRequestedEvent(1L, FcmPushType.BALANCE_RESULT, DATA, "투표 결과: 타이틀", "A vs B");

    @InjectMocks private FcmPushEventListener listener;

    @Mock private UserRepository userRepository;
    @Mock private FcmPushService fcmPushService;

    private User givenUser(boolean communityEnabled, boolean balanceResultEnabled) {
        User user = User.builder().id(1L).build();
        UserNoticeSetting setting = new UserNoticeSetting(user, false);
        setting.update(new NoticeSettingRequest(true, true, communityEnabled, false, null, balanceResultEnabled));
        user.setUserNoticeSetting(setting);
        given(userRepository.findByIdWithAppInfos(1L)).willReturn(Optional.of(user));
        return user;
    }

    @Test
    @DisplayName("결과 알림을 끄면 푸시를 보내지 않는다")
    void resultOff() {
        givenUser(true, false);

        listener.handleFcmPushRequested(EVENT);

        verifyNoInteractions(fcmPushService);
    }

    @Test
    @DisplayName("커뮤니티 활동 알림만 끈 사람은 푸시를 받는다")
    void communityOffOnly() {
        User user = givenUser(false, true);

        listener.handleFcmPushRequested(EVENT);

        verify(fcmPushService).send(user, FcmPushType.BALANCE_RESULT, DATA, "투표 결과: 타이틀", "A vs B");
    }
}
