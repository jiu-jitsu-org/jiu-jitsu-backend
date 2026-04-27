package com.jiujitsu.api.global.fcm.service;

import com.jiujitsu.api.global.fcm.client.FcmClient;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.dto.FcmMessage;
import com.jiujitsu.api.global.fcm.dto.FcmRequest;
import com.jiujitsu.api.global.fcm.dto.FcmNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmPushService {
    private final FcmClient fcmClient;

    @Value("${fcm.project-id}")
    private String projectId;

    public void send(String token, FcmPushType pushType) {
        FcmRequest request =
                FcmRequest.builder()
                        .message(
                                FcmMessage.builder()
                                        .token(token)
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
    }
}
