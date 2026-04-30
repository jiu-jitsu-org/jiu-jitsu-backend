package com.jiujitsu.api.global.fcm.service;

import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserAppInfo;
import com.jiujitsu.api.global.fcm.client.FcmClient;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.dto.FcmMessage;
import com.jiujitsu.api.global.fcm.dto.FcmRequest;
import com.jiujitsu.api.global.fcm.dto.FcmNotification;
import com.jiujitsu.api.global.fcm.entity.PushSendLog;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmPushService {
    private final FcmClient fcmClient;
    private final PushSendLogService pushSendLogService;

    @Value("${fcm.project-id}")
    private String projectId;

    public void send(User user, FcmPushType pushType) {
        boolean isSuccess = true;

        for (UserAppInfo appInfo : user.getAppInfos()) {
            try {
                FcmRequest request =
                        FcmRequest.builder()
                                .message(
                                        FcmMessage.builder()
                                                .token(appInfo.getToken())
                                                .fcmNotification(
                                                        FcmNotification.builder()
                                                                .title(pushType.getTitle())
                                                                .body(pushType.getBody())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build();

                fcmClient.sendPush(projectId, request);
            } catch (Exception e) {
                isSuccess = false;
            }

            // log
            PushSendLog pushSendLog = PushSendLog.builder()
                    .isSuccess(isSuccess)
                    .errorCode("0001")
                    .errorMessage("message")
                    .pushType(pushType)
                    .title(pushType.getTitle())
                    .body(pushType.getBody())
                    .userAppInfo(appInfo)
                    .build();

            pushSendLogService.insertPushSendLog(pushSendLog);
        }


    }
}
