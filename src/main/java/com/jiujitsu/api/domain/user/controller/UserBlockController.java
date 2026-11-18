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
            summary = "유저 차단",
            description = "특정 유저를 차단합니다. 이미 차단한 유저를 다시 요청해도 차단 상태로 남습니다 (연타·다른 기기 안전). "
                    + "응답은 항상 true (요청 뒤 차단 상태). 해제는 DELETE /user/block/{id}. 없는·탈퇴한 회원은 U0011."
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.SELF_BLOCK_NOT_ALLOWED, ErrorCode.MEMBER_NOT_FOUND})
    @PostMapping("/block/{id}")
    public Boolean block(
            @Parameter(name = "id", description = "차단할 유저 ID", required = true) @PathVariable(value = "id") Long id
    ) {
        return userBlockService.block(id);
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
