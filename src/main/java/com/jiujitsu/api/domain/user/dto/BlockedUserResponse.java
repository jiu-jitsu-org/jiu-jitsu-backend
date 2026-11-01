package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.user.entity.UserBlock;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "차단한 회원 정보")
public record BlockedUserResponse(
        @Schema(description = "차단된 유저 ID") Long userId,
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "프로필 이미지 URL (없으면 null)", nullable = true) String profileImageUrl,
        @Schema(description = "차단 일시") LocalDateTime blockedAt
) {
    public static BlockedUserResponse from(UserBlock block) {
        var blocked = block.getBlocked();
        String imageUrl = blocked.getProfileImageFile() != null
                ? blocked.getProfileImageFile().getImageUrl()
                : null;
        return new BlockedUserResponse(
                blocked.getId(),
                blocked.getNickname(),
                imageUrl,
                block.getCreatedAt()
        );
    }
}
