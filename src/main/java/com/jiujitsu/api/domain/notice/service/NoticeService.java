package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.notice.dto.NoticeListResponse;
import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.domain.notice.factory.NoticeFactory;
import com.jiujitsu.api.domain.notice.mapper.NoticeMapper;
import com.jiujitsu.api.domain.notice.repository.NoticeRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {
    private final NoticeRepository noticeRepository;
    private final NoticeFactory noticeFactory;
    private final NoticeMapper noticeMapper;
    private final AuthenticationFacade authenticationFacade;

    /**
     * 알림 목록 조회
     */
    @Transactional(readOnly = true)
    public List<NoticeListResponse> getNoticeList() {
        User user = authenticationFacade.getCurrentUser();
        List<Notice> notices = noticeRepository.findByUserId(user.getId());

        return notices.stream()
                .map(NoticeMapper::toNoticeListResponse)
                .sorted(Comparator.comparing(NoticeListResponse::createdAt).reversed())
                .toList();
    }

    /**
     * 알림 모두 읽음 처리
     */
    public Boolean readAllNotice() {
        User user = authenticationFacade.getCurrentUser();

        // 알림 조회
        List<Notice> notices = noticeRepository.findByUserId(user.getId());

        // 읽음 처리
        notices.forEach(Notice::setRead);
        return true;
    }
    /**
     * 알림 개별 읽음 처리
     */
    public Boolean readNotice(Long id) {
        User user = authenticationFacade.getCurrentUser();

        // 알림 조회
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.NOTICE_NOT_FOUND));

        // 권한 체크
        if (!Objects.equals(user.getId(), notice.getUserId())) {
            throw new ErrorException(ErrorCode.PERMISSION_DENIED);
        }

        // 읽음 처리
        notice.setRead();
        noticeRepository.save(notice);
        return true;
    }

    /**
     * 알림 저장
     */
    public void saveNotice(Long userId, FcmPushType pushType, Map<String, String> pushData) {
        Notice notice = noticeFactory.createNotice(userId, pushType, pushData);
        noticeRepository.save(notice);
    }
}
