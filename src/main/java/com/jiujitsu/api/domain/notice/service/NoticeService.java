package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.notice.dto.BoardNoticeSettingResponse;
import com.jiujitsu.api.domain.notice.dto.NoticeListResponse;
import com.jiujitsu.api.domain.notice.dto.NoticeSettingRequest;
import com.jiujitsu.api.domain.notice.dto.NoticeSettingResponse;
import com.jiujitsu.api.domain.notice.entity.ContentNoticeSetting;
import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;
import com.jiujitsu.api.domain.notice.factory.NoticeFactory;
import com.jiujitsu.api.domain.notice.mapper.NoticeMapper;
import com.jiujitsu.api.domain.notice.repository.ContentNoticeSettingRepository;
import com.jiujitsu.api.domain.notice.repository.NoticeRepository;
import com.jiujitsu.api.domain.notice.repository.UserNoticeSettingRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {
    private final NoticeRepository noticeRepository;
    private final NoticeFactory noticeFactory;
    private final AuthenticationFacade authenticationFacade;
    private final UserNoticeSettingRepository userNoticeSettingRepository;
    private final ContentNoticeSettingRepository contentNoticeSettingRepository;
    private final ContentRepository contentRepository;

    /**
     * 알림 목록 조회
     */
    @Transactional(readOnly = true)
    public List<NoticeListResponse> getNoticeList() {
        User user = authenticationFacade.getCurrentUser();

        return noticeRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(NoticeMapper::toNoticeListResponse)
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
        return true;
    }

    /**
     * 알림 저장
     */
    public void saveNotice(Long userId, FcmPushType pushType, Map<String, String> pushData) {
        Notice notice = noticeFactory.createNotice(userId, pushType, pushData);
        noticeRepository.save(notice);
    }

    /**
     * 알림 수신동의여부 조회
     */
    public NoticeSettingResponse getNoticeSetting() {
        User user = authenticationFacade.getCurrentUser();
        return NoticeMapper.toNoticeSettingResponse(user.getUserNoticeSetting());
    }

    /**
     * 알림 수신동의여부 저장
     */
    public NoticeSettingResponse saveNoticeSetting(NoticeSettingRequest request) {
        User user = authenticationFacade.getCurrentUser();

        UserNoticeSetting userNoticeSetting = user.getUserNoticeSetting();
        userNoticeSetting.update(request);

        userNoticeSettingRepository.save(userNoticeSetting);

        return NoticeMapper.toNoticeSettingResponse(userNoticeSetting);
    }

    /**
     * 게시물 알림 설정 조회
     * 알림 설정은 Content 단위라 게시글/밸런스 게임 모두 같은 API 를 사용한다.
     */
    @Transactional(readOnly = true)
    public BoardNoticeSettingResponse getBoardNoticeSetting(Long id) {
        User user = authenticationFacade.getCurrentUser();
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.CONTENT_NOT_FOUND));

        ContentNoticeSetting setting = findOrDefaultSetting(user.getId(), content.getId());

        return NoticeMapper.toBoardNoticeSettingResponse(id, setting);
    }

    /**
     * 게시물 알림 설정 토글 (댓글/좋아요/게시물 알림 전체 켜기/끄기)
     */
    public BoardNoticeSettingResponse toggleBoardNoticeSetting(Long id) {
        User user = authenticationFacade.getCurrentUser();
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.CONTENT_NOT_FOUND));

        ContentNoticeSetting setting = findOrDefaultSetting(user.getId(), content.getId());

        setting.toggle();
        contentNoticeSettingRepository.save(setting);

        return NoticeMapper.toBoardNoticeSettingResponse(id, setting);
    }

    // 저장된 설정이 없으면 기본값(ON) 설정을 만들어 반환 (아직 저장하지 않음)
    private ContentNoticeSetting findOrDefaultSetting(Long userId, Long contentId) {
        return contentNoticeSettingRepository
                .findByUserIdAndContentId(userId, contentId)
                .orElseGet(() -> ContentNoticeSetting.builder()
                        .userId(userId)
                        .contentId(contentId)
                        .enabled(true)
                        .build());
    }

    /**
     * 게시물별 알림 활성화 여부 확인 (알림 발송 전 호출)
     */
    @Transactional(readOnly = true)
    public boolean isContentNoticeEnabled(Long userId, Long contentId) {
        return contentNoticeSettingRepository.findByUserIdAndContentId(userId, contentId)
                .map(ContentNoticeSetting::getEnabled)
                .orElse(true);
    }
}
