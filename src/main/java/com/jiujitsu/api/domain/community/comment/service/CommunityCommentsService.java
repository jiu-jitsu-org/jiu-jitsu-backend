package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.*;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentReactionRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.comment.repository.ReactionCountProjection;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Transactional
    public CommunityCommentsWriteResponse write(
            Long contentId,
            Long parentId,
            String body
    ) {
        // 로그인 확인
        authenticationFacade.checkCurrentUser();

        // parentId 0일경우 null 로 처리
        parentId = Objects.equals(parentId, 0L) ? null : parentId;

        // 컨텐츠 조회
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.CONTENT_NOT_FOUND));

        communityCommentsRepository.createComment(content, parentId, body);
        return new CommunityCommentsWriteResponse(true);
    }

    @Transactional(readOnly = true)
    public List<CommunityComments> listTopLevel(Long contentId) {
        return communityCommentsRepository.findCommentsByContentId(contentId);
    }

    @Transactional(readOnly = true)
    public List<CommunityComments> listChild(Long contentId, Long parentId) {
        return communityCommentsRepository.findByContentIdAndParentIdOrderByCreatedAtAsc(contentId, parentId);
    }

    @Transactional(readOnly = true)
    public CommunityCommentsListResponse fetchCommentsList(Integer contentId) {
        List<CommunityCommentsItem> returnValue = new java.util.ArrayList<>(List.of());
        List<CommunityComments> parentsList = communityCommentsRepository.findCommentsByContentId(contentId.longValue());

        parentsList.forEach(communityComment -> {
            List<CommunityComments> childList = communityCommentsRepository.findByContentIdAndParentIdOrderByCreatedAtAsc(contentId.longValue(), communityComment.getId());
            List<ChildCommentItem> childListDto = new java.util.ArrayList<>(List.of());

            childList.forEach(childComment -> {
                childListDto.add(
                        new ChildCommentItem(
                                childComment.getId(),
                                childComment.getContent().getId(),
                                childComment.getParentId(),
                                childComment.getBody(),
                                new CommentAuthor(
                                        childComment.getCreatedBy().getId(),
                                        childComment.getCreatedBy().getNickname(),
                                        childComment.getCreatedBy().getProfileImageUrl()
                                ),
                                childComment.getCreatedAt(),
                                childComment.getUpdatedAt()
                        )
                );
            });

            returnValue.add(
                    new CommunityCommentsItem(
                            communityComment.getId(),
                            communityComment.getContent().getId(),
                            communityComment.getParentId(),
                            communityComment.getBody(),
                            new CommentAuthor(
                                    communityComment.getCreatedBy().getId(),
                                    communityComment.getCreatedBy().getNickname(),
                                    communityComment.getCreatedBy().getProfileImageUrl()
                            ),
                            communityComment.getCreatedAt(),
                            communityComment.getUpdatedAt(),
                            childListDto
                    )
            );
        });

        return new CommunityCommentsListResponse(returnValue);
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
