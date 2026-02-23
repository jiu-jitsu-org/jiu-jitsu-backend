package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.*;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentReactionRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.comment.repository.ReactionCountProjection;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
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

    @Transactional
    public CommunityCommentsWriteResponse write(
            Long postId,
            Long parentId,
            String body
    ) {
        // Content 유효성 체크
        if (!contentRepository.existsById(postId)) {
            throw new ErrorException(ErrorCode.CONTENT_NOT_FOUND);
        }

        communityCommentsRepository.createComment(
                postId,
                parentId,
                body
        );
        return new CommunityCommentsWriteResponse(true);
    }

    @Transactional(readOnly = true)
    public List<CommunityComments> listTopLevel(Long postId) {
        return communityCommentsRepository.findCommentsByPostId(postId);
    }

    @Transactional(readOnly = true)
    public List<CommunityComments> listChild(Long postId, Long parentId) {
        return communityCommentsRepository.findByPostIdAndParentIdOrderByCreatedAtAsc(postId, parentId);
    }

    @Transactional(readOnly = true)
    public CommunityCommentsListResponse fetchCommentsList(Integer postId) {
        List<CommunityCommentsItem> returnValue = new java.util.ArrayList<>(List.of());
        List<CommunityComments> parentsList = communityCommentsRepository.findCommentsByPostId(postId.longValue());


        parentsList.forEach(communityComment -> {
            List<CommunityComments> childList = communityCommentsRepository.findByPostIdAndParentIdOrderByCreatedAtAsc(postId.longValue(), communityComment.getId());
            List<ChildCommentItem> childListDto = new java.util.ArrayList<>(List.of());

            childList.forEach(childComment -> {
                childListDto.add(
                        new ChildCommentItem(
                                childComment.getId(),
                                childComment.getPostId(),
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
                            communityComment.getPostId(),
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
        var rows = commentReactionRepository.countByCommentIdsGroupByType(commentIds);
        var map = rows.stream().collect(Collectors.toMap(
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
