package com.jiujitsu.api.domain.boot_strap.controller;

import com.jiujitsu.api.domain.boot_strap.dto.AppVersionResponse;
import com.jiujitsu.api.domain.boot_strap.dto.BootStrapRequest;
import com.jiujitsu.api.domain.boot_strap.dto.BootStrapResponse;
import com.jiujitsu.api.domain.boot_strap.service.AppVersionService;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "[SYS] AppVersion", description = "강제업데이트, 선택업데이트 >> 26.07.31 GitHubWebhooks main branch test")
@CommonApiResponses
@RestController
@RequestMapping("/bootstrap")
@RequiredArgsConstructor
public class BootStrapController {

    private final AppVersionService appVersionService;

    @Operation(
            summary = "앱 버전 체크",
            description = "강제업데이트 유무 판단 및 최소 버전은 무조건 강제 업데이트 되도록"
    )
    @GetMapping("/info")
    public BootStrapResponse bootStrapInfo(
            @Parameter(description = "Name of the Mobile OS", example = "ANDROID / IOS")
            @RequestParam(defaultValue = "ANDROID") String osName
    ) {
        AppVersionResponse appVersion = appVersionService.checkAppVersion(new BootStrapRequest(osName));

        log.info("AppVersionService checkAppVersion Success!!!!!");
        return new BootStrapResponse(
                appVersion,
                "여기는 테스트 데이터 =)"
        );
    }

}
