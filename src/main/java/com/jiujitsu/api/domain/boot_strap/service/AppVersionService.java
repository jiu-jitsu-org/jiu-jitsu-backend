package com.jiujitsu.api.domain.boot_strap.service;

import com.jiujitsu.api.domain.boot_strap.dto.AdminAppVersionResponse;
import com.jiujitsu.api.domain.boot_strap.dto.AdminAppVersionUpsertRequest;
import com.jiujitsu.api.domain.boot_strap.dto.AppVersionResponse;
import com.jiujitsu.api.domain.boot_strap.dto.BootStrapRequest;
import com.jiujitsu.api.domain.boot_strap.entity.AppVersion;
import com.jiujitsu.api.domain.boot_strap.entity.OsType;
import com.jiujitsu.api.domain.boot_strap.repository.AppVersionRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppVersionService {

    private final AppVersionRepository appVersionRepository;

    public AppVersionResponse checkAppVersion(BootStrapRequest request) {
        AppVersion version = appVersionRepository.findByOsName(request.osName())
                .orElseThrow(() -> {
                    log.error("AppVersionService checkAppVersion findByOsName isEmpty");
                    return new ErrorException(ErrorCode.NO_APP_VERSION_DATA);
                });
        return new AppVersionResponse(version.getMinVersion(), version.getNowVersion(), version.isNeedForceUpdate());
    }

    @Transactional(readOnly = true)
    public List<AdminAppVersionResponse> getAllVersions() {
        return appVersionRepository.findAll().stream()
                .map(v -> new AdminAppVersionResponse(v.getOsName(), v.getMinVersion(), v.getNowVersion(), v.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public AdminAppVersionResponse upsertVersion(OsType osType, AdminAppVersionUpsertRequest request) {
        if (compareVersions(request.minVersion(), request.nowVersion()) > 0) {
            throw new ErrorException(ErrorCode.APP_VERSION_MIN_EXCEEDS_NOW);
        }
        AppVersion version = appVersionRepository.findByOsName(osType.name())
                .orElse(AppVersion.builder().osName(osType.name()).needForceUpdate(false).build());
        version.update(request.minVersion(), request.nowVersion());
        AppVersion saved = appVersionRepository.save(version);
        return new AdminAppVersionResponse(saved.getOsName(), saved.getMinVersion(), saved.getNowVersion(), saved.getUpdatedAt());
    }

    /** 버전 문자열 비교: v1 > v2 이면 양수, v1 < v2 이면 음수, 같으면 0 */
    private static int compareVersions(String v1, String v2) {
        String[] p1 = v1.split("\\.");
        String[] p2 = v2.split("\\.");
        int len = Math.max(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < p1.length ? Integer.parseInt(p1[i]) : 0;
            int n2 = i < p2.length ? Integer.parseInt(p2[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }
}
