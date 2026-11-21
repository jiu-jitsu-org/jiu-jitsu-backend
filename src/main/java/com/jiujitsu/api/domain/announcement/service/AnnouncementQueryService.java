package com.jiujitsu.api.domain.announcement.service;

import com.jiujitsu.api.domain.announcement.dto.AnnouncementDetailResponse;
import com.jiujitsu.api.domain.announcement.dto.AnnouncementListResponse;
import com.jiujitsu.api.domain.announcement.mapper.AnnouncementMapper;
import com.jiujitsu.api.domain.announcement.repository.AnnouncementRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementQueryService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementMapper announcementMapper;
    private final Clock clock;

    // 정렬은 서버 고정(게시 시각 최신순) — 클라이언트가 보낸 sort 는 버리고 page · size 만 쓴다
    public Page<AnnouncementListResponse> getList(Pageable pageable) {
        Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return announcementRepository.findPublished(LocalDateTime.now(clock), pageOnly)
                .map(announcementMapper::toListResponse);
    }

    // 게시 전(null · 미래) 공지는 없는 것과 같게 404
    public AnnouncementDetailResponse getDetail(Long announcementId) {
        return announcementRepository.findPublishedById(announcementId, LocalDateTime.now(clock))
                .map(announcementMapper::toDetailResponse)
                .orElseThrow(() -> new ErrorException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
    }
}
