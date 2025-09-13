package com.jiujitsu.api.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "로그아웃 응답")
public class LogoutResponse {

    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "로그아웃이 완료되었습니다")
    private String message;
}
