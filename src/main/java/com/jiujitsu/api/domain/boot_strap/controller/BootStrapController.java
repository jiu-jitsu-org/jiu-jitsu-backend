package com.jiujitsu.api.domain.boot_strap.controller;

import com.jiujitsu.api.domain.boot_strap.dto.AppNoticeInfo;
import com.jiujitsu.api.domain.boot_strap.dto.AppVersionResponse;
import com.jiujitsu.api.domain.boot_strap.dto.BootStrapRequest;
import com.jiujitsu.api.domain.boot_strap.dto.BootStrapResponse;
import com.jiujitsu.api.domain.boot_strap.service.AppNoticeService;
import com.jiujitsu.api.domain.boot_strap.service.AppVersionService;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[SYS] Bootstrap", description = "앱 버전 체크 · 점검/긴급 공지")
@CommonApiResponses
@RestController
@RequestMapping("/bootstrap")
@RequiredArgsConstructor
public class BootStrapController {

    private final AppVersionService appVersionService;
    private final AppNoticeService appNoticeService;

    @Operation(
            summary = "앱 시작 정보 조회",
            description = "앱 버전 체크(강제/선택 업데이트) + 현재 활성 공지(없으면 notice = null)"
    )
    @GetMapping("/info")
    public BootStrapResponse bootStrapInfo(
            @Parameter(description = "Name of the Mobile OS", example = "ANDROID / IOS")
            @RequestParam(defaultValue = "ANDROID") String osName
    ) {
        AppVersionResponse appVersion = appVersionService.checkAppVersion(new BootStrapRequest(osName));
        AppNoticeInfo notice = appNoticeService.getCurrentNotice().orElse(null);
        return new BootStrapResponse(appVersion, notice);
    }

}
