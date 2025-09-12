package com.jiujitsu.api.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "프로필 업데이트 응답")
public class UpdateProfileResponse {

    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "프로필이 성공적으로 업데이트되었습니다")
    private String message;

    @Schema(description = "업데이트된 사용자 정보")
    private UserInfo userInfo;

    @Getter
    @AllArgsConstructor
    @Schema(description = "사용자 정보")
    public static class UserInfo {
        @Schema(description = "사용자 ID", example = "1")
        private Long id;

        @Schema(description = "이메일", example = "user@example.com")
        private String email;

        @Schema(description = "닉네임", example = "홍길동")
        private String nickname;

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        private String profileImageUrl;

        @Schema(description = "SNS 제공자", example = "KAKAO")
        private String snsProvider;
    }
}
