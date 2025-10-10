package com.jiujitsu.api.domain.boot_strap.service;

import com.jiujitsu.api.domain.boot_strap.dto.AppVersionResponse;
import com.jiujitsu.api.domain.boot_strap.dto.BootStrapRequest;
import com.jiujitsu.api.domain.boot_strap.dto.BootStrapResponse;
import com.jiujitsu.api.domain.boot_strap.repository.AppVersionRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppVersionService {

    private final AppVersionRepository appVersionRepository;

    public AppVersionResponse checkAppVersion(BootStrapRequest request) {
        if (appVersionRepository
                .findByOsName(request.osName()).isEmpty()) {
            throw new ErrorException(ErrorCode.NO_APP_VERSION_DATA);
        } else {
            return new AppVersionResponse(
                    appVersionRepository
                            .findByOsName(request.osName()).get().getMinVersion(),
                    appVersionRepository
                            .findByOsName(request.osName()).get().getNowVersion(),
                    appVersionRepository
                            .findByOsName(request.osName()).get().isNeedForceUpdate()
            );
        }
    }
}
