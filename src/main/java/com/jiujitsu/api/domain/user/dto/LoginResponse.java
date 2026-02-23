package com.jiujitsu.api.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "로그인 응답")
public class LoginResponse {

    private boolean token;

    private UserResponse user;

    public static class UserResponse {
        private String id;
        private String username;
        private String role;
    }
}
