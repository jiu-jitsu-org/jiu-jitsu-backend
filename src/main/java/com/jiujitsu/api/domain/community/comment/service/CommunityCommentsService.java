package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.*;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentReactionRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.comment.repository.ReactionCountProjection;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserAppInfo;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.fcm.service.FcmPushService;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityCommentsService {

    private final CommunityCommentReactionRepository commentReactionRepository;
    private final CommunityCommentsRepository communityCommentsRepository;
    private final ContentRepository contentRepository;
    private final AuthenticationFacade authenticationFacade;
    private final FcmPushService fcmPushService;

    @Transactional
    public CommunityCommentsWriteResponse write(
            Long contentId,
            Long parentId,
            String body
    ) {
        // 로그인 확인
        authenticationFacade.checkCurrentUser();

        // 컨텐츠 조회
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.CONTENT_NOT_FOUND));

        communityCommentsRepository.createComment(content, parentId, body);

        try {
            // push 발송
            // 1. 작성자 조회
            User user = content.getCreatedBy();
            List<UserAppInfo> appInfos = user.getAppInfos();

            //todo:
            // 1. 본인이 작성한 글이면 제외
            // 2. 발송 서비스 호출
            // 3. 앱에서 토큰 받아서 테스트 진행 필요
            appInfos.forEach(info -> fcmPushService.send(info.getToken(), FcmPushType.NEW_COMMENTS));
        } catch (Exception e) {
            log.warn("FCM 푸시 발송 실패 (contentId={}): {}", contentId, e.getMessage());
        }

        return new CommunityCommentsWriteResponse(true);
    }

    @Transactional(readOnly = true)
    public CommunityCommentsListResponse fetchCommentsList(Long contentId) {
        List<CommunityComments> parentsList = communityCommentsRepository.findCommentsByContentId(contentId);

        List<CommunityCommentsItem> items = parentsList.stream()
                .map(parent -> {
                    List<ChildCommentItem> children = communityCommentsRepository
                            .findByContentIdAndParentIdOrderByCreatedAtAsc(contentId, parent.getId())
                            .stream()
                            .map(child -> new ChildCommentItem(
                                    child.getId(),
                                    child.getContent().getId(),
                                    child.getParentId(),
                                    child.getBody(),
                                    toCommentAuthor(child.getCreatedBy()),
                                    child.getCreatedAt(),
                                    child.getUpdatedAt()
                            ))
                            .toList();

                    return new CommunityCommentsItem(
                            parent.getId(),
                            parent.getContent().getId(),
                            parent.getParentId(),
                            parent.getBody(),
                            toCommentAuthor(parent.getCreatedBy()),
                            parent.getCreatedAt(),
                            parent.getUpdatedAt(),
                            children
                    );
                })
                .toList();

        return new CommunityCommentsListResponse(items);
    }

    private CommentAuthor toCommentAuthor(User user) {
        return new CommentAuthor(user.getId(), user.getNickname(), user.getProfileImageUrl());
    }

    public Map<Long, ReactionCountProjection> getReactionCount(Collection<Long> commentIds) {
        List<ReactionCountProjection> rows = commentReactionRepository.countByCommentIdsGroupByType(commentIds);
        Map<Long, ReactionCountProjection> map = rows.stream().collect(Collectors.toMap(
                ReactionCountProjection::getCommentId, Function.identity()
        ));

        for (Long id : commentIds) {
            map.computeIfAbsent(id, k -> new ReactionCountProjection() {
                @Override
                public Long getCommentId() {
                    return k;
                }

                @Override
                public long getLikeCnt() {
                    return 0L;
                }

                @Override
                public long getDislikeCnt() {
                    return 0L;
                }
            });
        }
        return map;
    }
}
