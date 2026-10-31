package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.community.board.service.BoardHideService;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.service.ReportService;
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
import com.jiujitsu.api.domain.user.service.UserBlockService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final CommunityCommentsRepository communityCommentsRepository;
    private final UserBlockService userBlockService;
    private final BoardHideService boardHideService;
    private final ReportService reportService;

    /**
     * 안 읽은 알림 수 조회 (종 아이콘 dot 용)
     * - 집계 기준은 목록 조회와 동일 (차단·숨기기·신고 필터 적용)
     */
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User user = authenticationFacade.getCurrentUser();
        Set<Long> excludedContentIds = getExcludedContentIds();
        List<Long> blockedUserIds = userBlockService.getBlockedUserIds();

        return noticeRepository.findByUserIdAndIsReadFalse(user.getId())
                .stream()
                .filter(n -> isVisible(n, blockedUserIds, excludedContentIds))
                .count();
    }

    /**
     * 알림 목록 조회
     * - 차단·숨기기·신고 필터 적용
     * - 각 알림의 대상(게시글·댓글·대댓글) 삭제 여부를 deletedStatus 로 반환
     */
    @Transactional(readOnly = true)
    public List<NoticeListResponse> getNoticeList() {
        User user = authenticationFacade.getCurrentUser();
        Set<Long> excludedContentIds = getExcludedContentIds();
        List<Long> blockedUserIds = userBlockService.getBlockedUserIds();

        List<Notice> notices = noticeRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(n -> isVisible(n, blockedUserIds, excludedContentIds))
                .toList();

        // 존재하는 contentId 일괄 조회
        Set<Long> contentIds = notices.stream()
                .map(Notice::getData)
                .filter(Objects::nonNull)
                .map(Long::parseLong)
                .collect(Collectors.toSet());
        Set<Long> existingContentIds = contentIds.isEmpty()
                ? Set.of()
                : contentRepository.findAllById(contentIds).stream()
                        .map(Content::getId)
                        .collect(Collectors.toSet());

        // 존재하는 commentId(refId) 일괄 조회
        Set<Long> refIds = notices.stream()
                .map(Notice::getRefId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CommunityComments> commentByRefId = refIds.isEmpty()
                ? Map.of()
                : communityCommentsRepository.findAllById(refIds).stream()
                        .collect(Collectors.toMap(CommunityComments::getId, c -> c));

        return notices.stream()
                .map(n -> NoticeMapper.toNoticeListResponse(n,
                        computeDeletedStatus(n, existingContentIds, commentByRefId)))
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
    public void saveNotice(Long userId, FcmPushType pushType, Map<String, String> pushData,
                           Long senderUserId, Long refId) {
        Notice notice = noticeFactory.createNotice(userId, pushType, pushData, senderUserId, refId);
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

    /**
     * 게시물 목록 > 알림을 끈 컨텐츠 ID 조회 (설정이 없으면 기본 활성화이므로 끈 것만 반환)
     */
    @Transactional(readOnly = true)
    public Set<Long> getNoticeDisabledContentIds(Long userId, List<Long> contentIds) {
        if (contentIds.isEmpty()) {
            return Set.of();
        }
        return contentNoticeSettingRepository.findByUserIdAndContentIdIn(userId, contentIds).stream()
                .filter(setting -> !setting.getEnabled())
                .map(ContentNoticeSetting::getContentId)
                .collect(Collectors.toSet());
    }

    // ── private helpers ──────────────────────────────────────────────────────

    /** 현재 유저의 숨기기 + 신고 게시글 contentId 합집합 */
    private Set<Long> getExcludedContentIds() {
        List<Long> hidden = boardHideService.getHiddenContentIds();
        List<Long> reported = reportService.getReportedTargetIds(ReportType.BOARD);
        return Stream.concat(hidden.stream(), reported.stream()).collect(Collectors.toSet());
    }

    /** 차단·숨기기·신고 기준으로 알림 노출 여부 판단 */
    private boolean isVisible(Notice n, List<Long> blockedUserIds, Set<Long> excludedContentIds) {
        if (n.getSenderUserId() != null && blockedUserIds.contains(n.getSenderUserId())) {
            return false;
        }
        if (n.getData() != null && excludedContentIds.contains(Long.parseLong(n.getData()))) {
            return false;
        }
        return true;
    }

    /**
     * 알림의 대상(게시글·댓글·대댓글) 삭제 상태 계산
     * - null     : 정상 (이동 가능)
     * - "BOARD"  : 게시글 삭제됨
     * - "COMMENT": 댓글 삭제됨 (게시글 존재)
     * - "REPLY"  : 대댓글 삭제됨 (게시글 존재)
     */
    private String computeDeletedStatus(Notice notice,
                                        Set<Long> existingContentIds,
                                        Map<Long, CommunityComments> commentByRefId) {
        if (notice.getData() == null) return null;

        Long contentId = Long.parseLong(notice.getData());
        if (!existingContentIds.contains(contentId)) {
            return "BOARD";
        }

        if (notice.getRefId() != null) {
            CommunityComments comment = commentByRefId.get(notice.getRefId());
            if (comment == null || comment.isDeleted()) {
                return switch (notice.getPushType()) {
                    case NEW_COMMENTS, COMMENTS_LIKE -> "COMMENT";
                    case NEW_CHILD_COMMENTS -> "REPLY";
                    default -> null;
                };
            }
        }
        return null;
    }
}
