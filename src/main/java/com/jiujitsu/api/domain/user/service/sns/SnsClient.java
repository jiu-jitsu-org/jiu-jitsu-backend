package com.jiujitsu.api.domain.user.service.sns;

import com.jiujitsu.api.domain.user.dto.SnsUserInfo;

public interface SnsClient {
    SnsUserInfo getUserInfo(String accessToken);

    /**
     * SNS 계정 연결 해제.
     * - Kakao: Admin Key + snsId로 서버 사이드 unlink
     * - Apple: authorizationCode → refresh_token 교환 후 revoke
     * - Google: 기본 no-op (앱 심사 강제 요건 없음)
     *
     * @param snsId             users.snsId (각 SNS의 고유 사용자 ID)
     * @param authorizationCode Apple 전용. iOS에서 Sign in with Apple 시 발급된 authorization_code.
     *                          Kakao/Google은 null 전달.
     */
    default void unlink(String snsId, String authorizationCode) {
        // no-op
    }
}
