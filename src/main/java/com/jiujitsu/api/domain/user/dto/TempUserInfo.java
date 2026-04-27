package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.user.entity.SnsProvider;

public record TempUserInfo(
        String snsId,
        String email,
        SnsProvider snsProvider
) {
}
