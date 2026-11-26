package com.jiujitsu.api.domain.boot_strap.service;

import com.jiujitsu.api.domain.boot_strap.dto.AdminAppNoticeRequest;
import com.jiujitsu.api.domain.boot_strap.dto.AdminAppNoticeResponse;
import com.jiujitsu.api.domain.boot_strap.dto.AppNoticeInfo;
import com.jiujitsu.api.domain.boot_strap.entity.AppNotice;
import com.jiujitsu.api.domain.boot_strap.repository.AppNoticeRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppNoticeService {

    private final AppNoticeRepository appNoticeRepository;

    public Optional<AppNoticeInfo> getCurrentNotice() {
        return appNoticeRepository.findCurrentNotice(LocalDateTime.now())
                .map(AppNoticeInfo::from);
    }

    public List<AdminAppNoticeResponse> getAllNotices() {
        return appNoticeRepository.findAll().stream()
                .map(AdminAppNoticeResponse::from)
                .toList();
    }

    @Transactional
    public AdminAppNoticeResponse create(AdminAppNoticeRequest request) {
        AppNotice notice = AppNotice.builder()
                .title(request.title())
                .body(request.body())
                .link(request.link())
                .blockApp(request.blockApp())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .active(request.active())
                .build();
        return AdminAppNoticeResponse.from(appNoticeRepository.save(notice));
    }

    @Transactional
    public AdminAppNoticeResponse update(Long id, AdminAppNoticeRequest request) {
        AppNotice notice = appNoticeRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.APP_NOTICE_NOT_FOUND));
        notice.update(request.title(), request.body(), request.link(),
                request.blockApp(), request.startAt(), request.endAt(), request.active());
        return AdminAppNoticeResponse.from(notice);
    }

    @Transactional
    public void delete(Long id) {
        AppNotice notice = appNoticeRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.APP_NOTICE_NOT_FOUND));
        appNoticeRepository.delete(notice);
    }
}
