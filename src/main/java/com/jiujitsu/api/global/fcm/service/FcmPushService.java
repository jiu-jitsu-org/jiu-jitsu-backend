package com.jiujitsu.api.global.fcm.service;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserAppInfo;
import com.jiujitsu.api.domain.user.repository.UserAppInfoRepository;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import com.jiujitsu.api.global.fcm.entity.PushSendLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class FcmPushService {
    private final FirebaseMessaging firebaseMessaging;
    private final PushSendLogService pushSendLogService;
    private final UserAppInfoRepository userAppInfoRepository;

    public void send(User user, FcmPushType pushType, Map<String, String> data, String title, String body) {
        String actionType = data.get("type");
        String pushData = data.get("data");
        String commentId = data.get("commentId");   // 댓글 · 대댓글 · 댓글 좋아요 푸시만 — 앱이 그 댓글을 바로 보여 줌 (#271)

        List<UserAppInfo> unregisteredTokens = new ArrayList<>();

        for (UserAppInfo appInfo : user.getAppInfos()) {
            boolean isSuccess = true;
            String errorCode = null;
            String errorMessage = null;

            try {
                Message.Builder builder = Message.builder()
                        .setToken(appInfo.getToken())
                        .setNotification(
                                Notification.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build()
                        )
                        .putData("type", actionType)
                        .putData("data", pushData)
                        // 앱이 푸시 종류별 열림을 세는 값 (GA notice_type)
                        .putData("pushType", pushType.name());
                if (commentId != null) {
                    builder.putData("commentId", commentId);
                }
                Message message = builder.build();

                // 의미있는 response 값이 오지는 않지만, 추후 관리 차원에서 일단 변수로 받아놓음
                String response = firebaseMessaging.send(message);
            } catch (FirebaseMessagingException e) {
                isSuccess = false;
                errorCode = e.getErrorCode().toString();
                errorMessage = e.getMessage();
                // FCM이 무효라고 돌려준 토큰은 발송 후 삭제 목록에 추가 (Firebase 공식 권장)
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    unregisteredTokens.add(appInfo);
                }
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
                    .title(title)
                    .body(body)
                    .pushActionType(PushActionType.from(data.get("type")))
                    .data(pushData)
                    .userAppInfo(appInfo)
                    .build();

            pushSendLogService.insertPushSendLog(pushSendLog);
        }

        // 무효 토큰 삭제 (루프 완료 후 처리)
        if (!unregisteredTokens.isEmpty()) {
            userAppInfoRepository.deleteAll(unregisteredTokens);
        }
    }
}
