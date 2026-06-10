package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.CommentsListRequest;
import com.jiujitsu.api.domain.community.comment.dto.CommentsSortType;
import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsResponse;
import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsWriteRequest;
import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeRequest;
import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeResponse;
import com.jiujitsu.api.domain.community.comment.entity.CommentLike;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.factory.CommentFactory;
import com.jiujitsu.api.domain.community.comment.factory.CommentLikeFactory;
import com.jiujitsu.api.domain.community.comment.mapper.CommentLikeMapper;
import com.jiujitsu.api.domain.community.comment.mapper.CommentMapper;
import com.jiujitsu.api.domain.community.comment.repository.CommentLikeRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.community.comment.event.CommentNoticeEvent;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.util.AuthenticationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommunityCommentsService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommunityCommentsRepository communityCommentsRepository;
    private final ContentRepository contentRepository;
    private final AuthenticationFacade authenticationFacade;
    private final UserBlockService userBlockService;
    private final ApplicationEventPublisher eventPublisher;
    private final CommentFactory commentFactory;
    private final CommentMapper commentMapper;
    private final CommentLikeFactory commentLikeFactory;
    private final CommentLikeMapper commentLikeMapper;


    /**
     * 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CommunityCommentsResponse> getComments(CommentsListRequest request) {
        Long contentId = request.id();
        CommentsSortType sortType = request.sortType();

        // 컨텐츠 조회
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.CONTENT_NOT_FOUND));

        // 댓글 전체 리스트 조회(댓글+대댓글) - createdBy, content fetch join으로 N+1 방지
        List<Long> blockedUserIds = userBlockService.getBlockedUserIds();
        List<CommunityComments> comments = blockedUserIds.isEmpty()
                ? communityCommentsRepository.findByContentIdOrderByCreatedAtDesc(contentId)
                : communityCommentsRepository.findByContentIdExcludingBlockedUsersOrderByCreatedAtDesc(contentId, blockedUserIds);


        // 좋아요 조회(n+1 방지 > 전체 조회 후 mapping)
        List<Long> commentIds = comments.stream().map(CommunityComments::getId).toList();

        // 좋아요 수
        Map<Long, Long> likeCountMap = commentLikeRepository.countGroupByCommentIds(commentIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        // 좋아요 여부
        Set<Long> likedCommentIds = AuthenticationUtil.getCurrentUserId()
                .map(userId -> new HashSet<>(commentLikeRepository.findLikedCommentIds(commentIds, userId)))
                .orElse(new HashSet<>());

        // 작성자 여부
        User user = authenticationFacade.getCurrentUserOptional().orElse(null);

        // 전체 댓글 Response 로 mapping
        Map<Long, CommunityCommentsResponse> commentsMap = comments.stream()
                .collect(Collectors.toMap(
                        CommunityComments::getId,
                        c -> commentMapper.toCommunityCommentsResponse(
                                c,
                                new ArrayList<>(),    // 대댓글 하단에서 추가하기 위해 mutable list 사용
                                likeCountMap.getOrDefault(c.getId(), 0L),
                                likedCommentIds.contains(c.getId()),
                                Objects.equals(c.getCreatedBy(), user))
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
        return Objects.equals(sortType, CommentsSortType.CREATE_DESC)
                ? result.stream().sorted(Comparator.comparing(CommunityCommentsResponse::createdAt).reversed()).toList()
                : result.stream().sorted(Comparator.comparing(CommunityCommentsResponse::createdAt)).toList();
    }

    /**
     * 댓글 작성
     */
    public CommunityCommentsResponse createComment(CommunityCommentsWriteRequest request) {
        // 로그인 확인
        User user = authenticationFacade.getCurrentUser();

        // 컨텐츠 조회
        Content content = contentRepository.findById(request.contentId())
                .orElseThrow(() -> new ErrorException(ErrorCode.CONTENT_NOT_FOUND));

        // 대댓글일 경우 부모댓글 조회
        Long parentID = request.parentId() == null ? 0L : request.parentId();
        Optional<CommunityComments> parentComments = communityCommentsRepository.findById(parentID);


        // 댓글 entity 생성
        CommunityComments communityComments = commentFactory.createComments(content, parentID, request.body());
        communityCommentsRepository.save(communityComments);

        // 알림 설정 (커밋 후 FCM 발송 + 알림 저장)
        User boardWriter = content.getCreatedBy();

        // 알림1 - 게시글에 댓글 달렸을 때 게시글 작성자에게
        if (!Objects.equals(user.getId(), boardWriter.getId())) {
            eventPublisher.publishEvent(new CommentNoticeEvent(
                    boardWriter.getId(),
                    content.getId(),
                    FcmPushType.NEW_COMMENTS,
                    Map.of("type", FcmPushType.NEW_COMMENTS.getActionType().toString(),
                           "data", content.getId().toString())
            ));
        }

        // 알림2 - 댓글에 대댓글 달렸을 때 댓글 작성자에게
        if (parentComments.isPresent()) {
            if (!Objects.equals(user.getId(), parentComments.get().getCreatedBy().getId())) {
                eventPublisher.publishEvent(new CommentNoticeEvent(
                        parentComments.get().getCreatedBy().getId(),
                        content.getId(),
                        FcmPushType.NEW_CHILD_COMMENTS,
                        Map.of("type", FcmPushType.NEW_CHILD_COMMENTS.getActionType().toString(),
                               "data", content.getId().toString())
                ));
            }
        }

        return commentMapper.toCommunityCommentsResponse(communityComments, new ArrayList<>());
    }

    /**
     * 댓글 좋아요 등록
     */
    public CommentLikeResponse createCommentLike(CommentLikeRequest request) {
        // 로그인 유저 정보 조회
        User user = authenticationFacade.getCurrentUser();

        // 댓글 조회
        CommunityComments comment = communityCommentsRepository.findById(request.commentId())
                .orElseThrow(() -> new ErrorException(ErrorCode.COMMENT_NOT_FOUND));

        // 기존 좋아요 조회
        CommentLike newLike = null;
        Optional<CommentLike> existLike = commentLikeRepository.findByCommentIdAndCreatedBy(comment.getId(), user);

        if (existLike.isPresent()) {
            // 좋아요 취소
            commentLikeRepository.delete(existLike.get());
        } else {
            // 좋아요 등록
            newLike = commentLikeFactory.createCommentLike(comment);
            commentLikeRepository.save(newLike);

            if (!Objects.equals(user.getId(), comment.getCreatedBy().getId())) {
                eventPublisher.publishEvent(new CommentNoticeEvent(
                        comment.getCreatedBy().getId(),
                        comment.getContent().getId(),
                        FcmPushType.COMMENTS_LIKE,
                        Map.of("type", FcmPushType.COMMENTS_LIKE.getActionType().toString(),
                               "data", comment.getId().toString())
                ));
            }
        }

        return commentLikeMapper.toCommentLikeResponse(comment, newLike);
    }

    /**
     * 댓글 삭제
     */
    public void deleteComment(Long commentId) {
        CommunityComments comment = communityCommentsRepository.findById(commentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.COMMENT_NOT_FOUND));

        // 수정 권한 체크
        comment.validateOwner(authenticationFacade.getCurrentUser());

        communityCommentsRepository.delete(comment);
    }

    /**
     * 게시글 단건 > 댓글 카운트 조회
     */
    @Transactional(readOnly = true)
    public long getCountComments(Long contentId) {
        return communityCommentsRepository
                .countByContent_IdAndParentIdIsNull(contentId);
    }

    /**
     * 게시글 목록 > 댓글 카운트 조회
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> getContentsComments(List<Long> contentIds) {
        return communityCommentsRepository.countTopLevelCommentsByContentIds(contentIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
    }

    /**
     * 게시글 - 댓글id 조회
     */
    @Transactional(readOnly = true)
    public Set<Long> getUserCommentedContentIds(Long userId, List<Long> contentIds) {
        return communityCommentsRepository.findUserCommentedContentIds(userId, contentIds);
    }

}
