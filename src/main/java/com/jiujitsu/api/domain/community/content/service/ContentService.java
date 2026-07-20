package com.jiujitsu.api.domain.community.content.service;

import com.jiujitsu.api.domain.community.content.dto.ContentLikeResponse;
import com.jiujitsu.api.domain.community.content.dto.ContentSaveResponse;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentLike;
import com.jiujitsu.api.domain.community.content.entity.ContentSave;
import com.jiujitsu.api.domain.community.content.mapper.ContentLikeMapper;
import com.jiujitsu.api.domain.community.content.mapper.ContentSaveMapper;
import com.jiujitsu.api.domain.community.content.repository.ContentLikeRepository;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.community.content.repository.ContentSaveRepository;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.event.FcmPushEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ContentService {
    private final AuthenticationFacade authenticationFacade;
    private final ContentRepository contentRepository;
    private final ContentLikeRepository contentLikeRepository;
    private final ContentLikeMapper contentLikeMapper;
    private final ContentSaveRepository contentSaveRepository;
    private final ContentSaveMapper contentSaveMapper;
    private final FcmPushEventPublisher fcmPushEventPublisher;
    private final NoticeService noticeService;

    /**
     * 게시물 좋아요 등록/삭제
     */
    public ContentLikeResponse like(Long id) {
        // 로그인 유저 정보 조회
        User user = authenticationFacade.getCurrentUser();

        // 게시물 조회
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.CONTENT_NOT_FOUND));

        // 기존 좋아요 조회
        ContentLike newLike = null;
        Optional<ContentLike> existLike = contentLikeRepository.findByContentIdAndCreatedBy(id, user);

        if (existLike.isPresent()) {
            // 좋아요 취소
            contentLikeRepository.delete(existLike.get());
        } else {
            // 좋아요 등록
            newLike = ContentLike.builder().content(content).build();
            contentLikeRepository.save(newLike);

            if (!Objects.equals(user.getId(), content.getCreatedBy().getId())) {
                FcmPushType pushType = FcmPushType.CONTENTS_LIKE;

                Map<String, String> pushData = new HashMap<>();
                pushData.put("type", pushType.getActionType().toString());
                pushData.put("data", content.getId().toString());

                Long contentOwnerId = content.getCreatedBy().getId();
                if (noticeService.isContentNoticeEnabled(contentOwnerId, content.getId())) {
                    fcmPushEventPublisher.publish(contentOwnerId, pushType, pushData);
                    noticeService.saveNotice(contentOwnerId, pushType, pushData);
                }
            }
        }

        return contentLikeMapper.toContentLikeResponse(content, newLike);
    }

    /**
     * 게시글 목록 > 좋아요 카운트 조회
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> getContentLikeCount(List<Long> contentIds) {
        return contentLikeRepository.countContentLikeByContentIds(contentIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
    }

    /**
     * 게시물 저장 등록/삭제
     */
    public ContentSaveResponse save(Long id) {
        // 로그인 유저 정보 조회
        User user = authenticationFacade.getCurrentUser();

        // 게시물 조회
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.CONTENT_NOT_FOUND));

        // 기존 저장 조회
        ContentSave newSave = null;
        Optional<ContentSave> existSave = contentSaveRepository.findByContentIdAndCreatedBy(id, user);

        if (existSave.isPresent()) {
            // 저장 취소
            contentSaveRepository.delete(existSave.get());
        } else {
            // 저장 등록
            newSave = ContentSave.builder().content(content).build();
            contentSaveRepository.save(newSave);
        }

        return contentSaveMapper.toContentSaveResponse(content, newSave);
    }

    /**
     * 게시글 목록 > 저장 카운트 조회
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> getContentSaveCount(List<Long> contentIds) {
        return contentSaveRepository.countContentSaveByContentIds(contentIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
    }

    /**
     * 게시글 - 좋아요id 조회
     */
    @Transactional(readOnly = true)
    public Set<Long> getUserLikedContentIds(Long userId, List<Long> contentIds) {
        return contentLikeRepository.findUserLikedContentIds(userId, contentIds);
    }

    /**
     * 게시글 - 저장id 조회
     */
    @Transactional(readOnly = true)
    public Set<Long> getUserSavedContentIds(Long userId, List<Long> contentIds) {
        return contentSaveRepository.findUserSavedContentIds(userId, contentIds);
    }
}
