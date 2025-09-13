package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "사용자 프로필 조회 응답")
public class UserProfileResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImageUrl;

    @Schema(description = "SNS 제공자", example = "KAKAO")
    private SnsProvider snsProvider;

    @Schema(description = "사용자 역할", example = "USER")
    private String role;

    @Schema(description = "사용자 상태", example = "ACTIVE")
    private String status;

    public UserProfileResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.profileImageUrl = user.getProfileImageUrl();
        this.snsProvider = user.getSnsProvider();
        this.role = user.getRole().name();
        this.status = user.getStatus().name();
    }
}
