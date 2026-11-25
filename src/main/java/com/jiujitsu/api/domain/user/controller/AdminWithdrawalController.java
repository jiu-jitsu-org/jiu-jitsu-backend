package com.jiujitsu.api.domain.user.controller;

import com.jiujitsu.api.domain.user.dto.AdminWithdrawalListItem;
import com.jiujitsu.api.domain.user.service.AdminUserService;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.security.annotation.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin-withdrawal-controller", description = "관리자 탈퇴 관리 API")
@CommonApiResponses
@AdminOnly
@RestController
@RequestMapping("/admin/withdrawals")
@RequiredArgsConstructor
public class AdminWithdrawalController {

    private final AdminUserService adminUserService;

    @Operation(summary = "탈퇴 신청 목록", description = "탈퇴 신청한 회원 목록. deletedAt 최신순, 20개씩.")
    @GetMapping
    public Page<AdminWithdrawalListItem> getWithdrawals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminUserService.getWithdrawals(page, size);
    }
}
