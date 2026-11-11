package com.jiujitsu.api.domain.notice.entity;

import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 마감 전날 알림을 보내는 마감 종류
 */
@Getter
@RequiredArgsConstructor
public enum CompetitionDeadlineType {
    EARLY_BIRD(FcmPushType.COMPETITION_EARLY_BIRD_DEADLINE),     // 얼리버드(할인 단계) 마감
    REGISTRATION(FcmPushType.COMPETITION_REGISTRATION_DEADLINE); // 접수 마감

    private final FcmPushType pushType;
}
