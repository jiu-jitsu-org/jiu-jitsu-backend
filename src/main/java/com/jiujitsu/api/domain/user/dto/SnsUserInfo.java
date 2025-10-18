package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.user.entity.SnsProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SnsUserInfo {
    private String snsId;
    private String email;
    private SnsProvider snsProvider;
    private String nickname;
    private String profileImageUrl;

    public SnsUserInfo(String snsId, String email) {
        this.snsId = snsId;
        this.email = email;
    }

    public SnsUserInfo(String snsId, String email, String nickname) {
        this.snsId = snsId;
        this.email = email;
        this.nickname = nickname;
    }
}

