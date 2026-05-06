package com.jiujitsu.api.global.fcm.service;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserAppInfo;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import com.jiujitsu.api.global.fcm.entity.PushSendLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FcmPushService {
    private final FirebaseMessaging firebaseMessaging;
    private final PushSendLogService pushSendLogService;

    public void send(User user, FcmPushType pushType, Map<String, String> data) {
        String actionType = data.get("type");
        String pushData = data.get("data");

        for (UserAppInfo appInfo : user.getAppInfos()) {
            boolean isSuccess = true;
            String errorCode = null;
            String errorMessage = null;


            try {
                Message message = Message.builder()
                        .setToken(appInfo.getToken())
                        .setNotification(
                                Notification.builder()
                                        .setTitle(pushType.getTitle())
                                        .setBody(pushType.getBody())
                                        .build()
                        )
                        .putData("type", actionType)
                        .putData("data", pushData)
                        .build();

                // 의미있는 response 값이 오지는 않지만, 추후 관리 차원에서 일단 변수로 받아놓음
                String response = firebaseMessaging.send(message);
            } catch (FirebaseMessagingException e) {
                isSuccess = false;
                errorCode = e.getErrorCode().toString();
                errorMessage = e.getMessage();
            } catch (Exception e) {
                isSuccess = false;
                errorCode = ErrorCode.UNKNOWN.toString();
                errorMessage = e.getMessage();
            }

            // log
            PushSendLog pushSendLog = PushSendLog.builder()
                    .isSuccess(isSuccess)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .pushType(pushType)
                    .title(pushType.getTitle())
                    .body(pushType.getBody())
                    .pushActionType(PushActionType.from(data.get("type")))
                    .data(pushData)
                    .userAppInfo(appInfo)
                    .build();

            pushSendLogService.insertPushSendLog(pushSendLog);
        }


    }
}
