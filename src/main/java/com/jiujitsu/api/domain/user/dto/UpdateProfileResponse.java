package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.file.dto.ImageInfo;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "프로필 업데이트 응답")
public class UpdateProfileResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "프로필 이미지")
    private ImageInfo profileImage;

    @Schema(description = "SNS 제공자", example = "KAKAO")
    private SnsProvider snsProvider;

    public UpdateProfileResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.profileImage = ImageInfo.from(user.getProfileImageFile());
        this.snsProvider = user.getSnsProvider();
    }
}
