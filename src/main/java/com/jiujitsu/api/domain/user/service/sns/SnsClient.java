package com.jiujitsu.api.domain.user.service.sns;

import com.jiujitsu.api.domain.user.dto.SnsUserInfo;

public interface SnsClient {
    SnsUserInfo getUserInfo(String accessToken, String idToken);
}
