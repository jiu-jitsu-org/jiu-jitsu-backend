package com.jiujitsu.api.domain.user.controller;

import com.jiujitsu.api.domain.user.dto.AdminOwnerRequestResponse;
import com.jiujitsu.api.domain.user.dto.OwnerRequestRejectRequest;
import com.jiujitsu.api.domain.user.entity.OwnerVerificationStatus;
import com.jiujitsu.api.domain.user.service.OwnerVerificationService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Tag(name = "admin-owner-request-controller", description = "관리자 관장/사범 인증 신청 관리 API")
@CommonApiResponses
@RestController
@RequestMapping("/admin/owner-requests")
@RequiredArgsConstructor
public class AdminOwnerRequestController {

    private final OwnerVerificationService ownerVerificationService;

    @Operation(summary = "인증 신청 목록 조회", description = "상태별 관장/사범 인증 신청 목록을 신청 최신순으로 페이징 조회합니다. (sort 파라미터는 무시)")
    @GetMapping
    public Page<AdminOwnerRequestResponse> getRequests(
            @Parameter(description = "신청 상태 (기본 PENDING)")
            @RequestParam(defaultValue = "PENDING") OwnerVerificationStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ownerVerificationService.getRequests(status, pageable);
    }

    @Operation(summary = "인증 신청 승인", description = "신청자에게 관장/사범 권한을 부여합니다. 커뮤니티 프로필이 없으면 승인할 수 없습니다.")
    @ApiErrorCodeExamples({ErrorCode.OWNER_REQUEST_NOT_FOUND, ErrorCode.OWNER_REQUEST_NOT_PENDING, ErrorCode.REQUIRED_PROFILE})
    @PatchMapping("/{requestId}/approve")
    public void approve(
            @Parameter(name = "requestId", description = "신청 ID", required = true)
            @PathVariable Long requestId
    ) {
        ownerVerificationService.approve(requestId);
    }

    @Operation(summary = "인증 신청 거절", description = "신청을 거절합니다. 신청자는 다시 신청할 수 있습니다.")
    @ApiErrorCodeExamples({ErrorCode.OWNER_REQUEST_NOT_FOUND, ErrorCode.OWNER_REQUEST_NOT_PENDING})
    @PatchMapping("/{requestId}/reject")
    public void reject(
            @Parameter(name = "requestId", description = "신청 ID", required = true)
            @PathVariable Long requestId,
            @Valid @RequestBody(required = false) OwnerRequestRejectRequest request
    ) {
        ownerVerificationService.reject(requestId, request == null ? null : request.reason());
    }
}
