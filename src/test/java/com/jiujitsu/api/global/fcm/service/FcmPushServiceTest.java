package com.jiujitsu.api.global.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserAppInfo;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import com.jiujitsu.api.global.fcm.entity.PushSendLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmPushServiceTest {

    @InjectMocks private FcmPushService fcmPushService;

    @Mock private FirebaseMessaging firebaseMessaging;
    @Mock private PushSendLogService pushSendLogService;

    private User userWithDevice() {
        User user = User.builder().id(1L).appInfos(new ArrayList<>()).build();
        user.getAppInfos().add(UserAppInfo.builder().token("token-1").build());
        return user;
    }

    @Test
    @DisplayName("제목·본문을 넘기면 그 값으로 보내고 발송 기록에도 남긴다 (대회 마감 알림)")
    void send_withTitleAndBody() {
        fcmPushService.send(userWithDevice(), FcmPushType.COMPETITION_REGISTRATION_DEADLINE,
                Map.of("type", PushActionType.COMPETITION_DETAIL.name(), "data", "7"),
                "접수 마감 내일", "서울 오픈 접수가 내일 끝나요. 신청과 결제는 대회 공지에서");

        ArgumentCaptor<PushSendLog> log = ArgumentCaptor.forClass(PushSendLog.class);
        verify(pushSendLogService).insertPushSendLog(log.capture());
        assertThat(log.getValue().getTitle()).isEqualTo("접수 마감 내일");
        assertThat(log.getValue().getBody()).isEqualTo("서울 오픈 접수가 내일 끝나요. 신청과 결제는 대회 공지에서");
        assertThat(log.getValue().getPushActionType()).isEqualTo(PushActionType.COMPETITION_DETAIL);
        assertThat(log.getValue().getData()).isEqualTo("7");
    }
}
