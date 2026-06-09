package com.jiujitsu.api.domain.user.controller;

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

@Tag(name = "user-block-controller", description = "사용자 차단 API")
@CommonApiResponses
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserBlockController {

    private final UserBlockService userBlockService;

    @Operation(
            summary = "유저 차단/차단 해제",
            description = "특정 유저를 차단하거나 차단 해제합니다. 이미 차단된 유저를 다시 요청하면 차단이 해제됩니다. (true = 차단, false = 차단 해제)"
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.SELF_BLOCK_NOT_ALLOWED, ErrorCode.AUTHENTICATION_FAILED})
    @PostMapping("/block/{id}")
    public Boolean toggleBlock(
            @Parameter(name = "id", description = "차단할 유저 ID", required = true) @PathVariable Long id
    ) {
        return userBlockService.toggleBlock(id);
    }
}
