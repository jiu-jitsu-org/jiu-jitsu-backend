package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsResponse;
import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsWriteRequest;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.factory.CommentFactory;
import com.jiujitsu.api.domain.community.comment.mapper.CommentMapper;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentReactionRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.comment.repository.ReactionCountProjection;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.fcm.service.FcmPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    private final CommentFactory commentFactory;
    private final CommentMapper commentMapper;

    /**
     * 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CommunityCommentsResponse> getComments(Long contentId) {
        // 컨텐츠 조회
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));

        // 댓글 전체 리스트 조회(댓글+대댓글) > n+1 조회 이슈로 전체 조회 후 여기서 세팅...
        List<CommunityComments> comments = communityCommentsRepository.findByContentIdOrderByCreatedAtDesc(contentId);

        // 전체 댓글 Response로 mapping
        Map<Long, CommunityCommentsResponse> commentsMap = comments.stream()
                .collect(Collectors.toMap(
                        CommunityComments::getId,
                        c -> commentMapper.toCommunityCommentsResponse(c, new ArrayList<>())    // 대댓글 하단에서 추가하기 위해 mutable list 사용
                        ));

        // 결과에 댓글/대댓글 나눠 넣기
        List<CommunityCommentsResponse> result = new ArrayList<>();

        for (CommunityComments comment : comments) {
            CommunityCommentsResponse dto = commentsMap.get(comment.getId());

            if (comment.getParentId() == null || Objects.equals(comment.getParentId(), 0L)) {
                // 댓글 case(부모)
                result.add(dto);    // 바로 result 넣는다.
            } else {
                // 대댓글 case(자식)
                CommunityCommentsResponse parent = commentsMap.get(comment.getParentId());
                if (parent != null) {
                    // 댓글 > childrenList에 해당 데이터 넣는다.
                    parent.childrenList().add(dto);
                }
            }
        }

        // 부모 기준 정렬하여 return
        return result;
    }

    /**
     * 댓글 작성
     */
    @Transactional
    public CommunityCommentsResponse createComment(CommunityCommentsWriteRequest request) {
        // 로그인 확인
        authenticationFacade.checkCurrentUser();

        // 컨텐츠 조회
        Content content = contentRepository.findById(request.contentId())
                .orElseThrow(() -> new ErrorException(ErrorCode.CONTENT_NOT_FOUND));

        // 댓글 entity 생성
        CommunityComments communityComments = commentFactory.createComments(content, request.parentId(), request.body());
        communityCommentsRepository.save(communityComments);

        return commentMapper.toCommunityCommentsResponse(communityComments, new ArrayList<>());
    }

    /**
     * 리액션 카운트 조회
     */
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

    /**
     * 게시글 단건 > 댓글 카운트 조회
     */
    public long getCountComments(Long contentId) {
        return communityCommentsRepository
                .countByContent_IdAndParentIdIsNull(contentId);
    }

    /**
     * 게시글 목록 > 댓글 카운트 조회
     */
    public Map<Long, Long> getContentsComments(List<Long> contentIds) {
        return communityCommentsRepository.countTopLevelCommentsByContentIds(contentIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
    }
}
