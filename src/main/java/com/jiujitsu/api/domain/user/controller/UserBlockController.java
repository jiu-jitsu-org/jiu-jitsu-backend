package com.jiujitsu.api.domain.user.controller;

import com.jiujitsu.api.domain.user.dto.BlockedUserResponse;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.exception.annotation.LoginErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "user-block-controller", description = "사용자 차단 API")
@CommonApiResponses
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserBlockController {

    private final UserBlockService userBlockService;

    @Operation(summary = "차단한 회원 목록 조회",
            description = "내가 차단한 회원을 최근 차단순으로 반환합니다. 탈퇴 회원은 제외됩니다.")
    @LoginErrorExamples
    @GetMapping("/block")
    public List<BlockedUserResponse> getBlockedUsers() {
        return userBlockService.getBlockedUsers();
    }

    @Operation(
            summary = "유저 차단/차단 해제 (토글)",
            description = "특정 유저를 차단하거나 차단 해제합니다. 이미 차단된 유저를 다시 요청하면 차단이 해제됩니다. (true = 차단, false = 차단 해제)"
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.SELF_BLOCK_NOT_ALLOWED, ErrorCode.AUTHENTICATION_FAILED})
    @PostMapping("/block/{id}")
    public Boolean toggleBlock(
            @Parameter(name = "id", description = "차단할 유저 ID", required = true) @PathVariable(value = "id") Long id
    ) {
        return userBlockService.toggleBlock(id);
    }

    @Operation(summary = "차단 해제 전용",
            description = "특정 유저의 차단을 해제합니다. 이미 해제된 경우 무시합니다 (다중 기기 안전).")
    @LoginErrorExamples
    @DeleteMapping("/block/{id}")
    public void unblock(
            @Parameter(name = "id", description = "차단 해제할 유저 ID", required = true) @PathVariable(value = "id") Long id
    ) {
        userBlockService.unblock(id);
    }
}
