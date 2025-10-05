package com.jiujitsu.api.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SnsUserInfo {
    private String snsId;
    private String email;
    private String nickname;
    private String profileImageUrl;

    public SnsUserInfo(String snsId, String email) {
        this.snsId = snsId;
        this.email = email;
    }
}

