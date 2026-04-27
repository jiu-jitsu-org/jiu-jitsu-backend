package com.jiujitsu.api.domain.user.mapper;

import com.jiujitsu.api.domain.user.dto.AuthResponse;
import com.jiujitsu.api.domain.user.dto.LogoutResponse;
import com.jiujitsu.api.domain.user.dto.UserInfo;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {
    /**
     * 기존회원 access Token Response
     */
    public AuthResponse toAccessResponse(String accessToken, String refreshToken, UserInfo userInfo) {
        return new AuthResponse(
              false,
              null,
              accessToken,
              refreshToken,
              userInfo
        );
    }

    /**
     * 신규회원 temp Token Response
     */
    public AuthResponse toTempResponse(String tempToken) {
        return new AuthResponse(
                true,
                tempToken,
                null,
                null,
                null
        );
    }

    /**
     * 로그아웃 Response
     */
    public LogoutResponse toLogoutResponse() {
        return new LogoutResponse(true, "로그아웃이 완료되었습니다");
    }
}
