package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * 커뮤니티 알림의 미리보기 본문 — 알림함 카드 body 와 푸시 본문이 같은 규칙을 쓰게 한 곳에 둔다 (#196)
 * - 내 게시글에 댓글 · 좋아요 : 게시글 제목 (밸런스 게임은 게시글이 없어 null)
 * - 내 댓글에 대댓글          : 대댓글이 달린 내 댓글
 * - 내 댓글에 좋아요          : 좋아요 받은 댓글
 * - 댓글이 삭제·숨김(신고 포함)이면 null — 지운 글이 알림함·잠금화면에 다시 보이지 않게
 *
 * 규칙은 {@link #body} 하나다. 어디서 값을 읽을지만 호출 쪽이 넘긴다
 * - 알림함 목록(NoticeService): 화면 단위로 일괄 조회한 Map 을 넘긴다 (N+1 없음)
 * - 푸시(CommentNoticeEventListener, LikeNoticeService): 이 빈의 {@link #of} 가 리포지토리 단건 조회를 넘긴다
 */
@Component
@RequiredArgsConstructor
public class NoticePreview {

    private final BoardRepository boardRepository;
    private final CommunityCommentsRepository communityCommentsRepository;

    /**
     * 알림 종류별 미리보기 본문 (규칙 한 곳)
     *
     * @param contentId         게시글·밸런스 게임 contentId
     * @param refId             NEW_CHILD_COMMENTS = 새 대댓글(받는 사람이 쓴 댓글 = 태그한 대댓글, 없으면 부모 댓글을 보여 줌), COMMENTS_LIKE = 좋아요 받은 댓글
     * @param boardTitleOf      contentId → 게시글 제목 (없으면 null)
     * @param commentOf         댓글 id → 댓글 (없으면 null)
     */
    public static String body(FcmPushType pushType, Long contentId, Long refId,
                              Function<Long, String> boardTitleOf,
                              Function<Long, CommunityComments> commentOf) {
        return switch (pushType) {
            case NEW_COMMENTS, CONTENTS_LIKE -> contentId == null ? null : boardTitleOf.apply(contentId);
            case NEW_CHILD_COMMENTS -> {
                if (refId == null) yield null;
                CommunityComments reply = commentOf.apply(refId);
                Long replied = reply == null ? null : reply.repliedCommentId();
                if (replied == null || replied == 0) yield null;
                yield commentPreview(commentOf.apply(replied));
            }
            case COMMENTS_LIKE -> refId == null ? null : commentPreview(commentOf.apply(refId));
            default -> null;
        };
    }

    /** 알림 하나의 미리보기 — 푸시를 보낼 때 (리포지토리 단건 조회로 {@link #body}) */
    public String of(FcmPushType pushType, Long contentId, Long refId) {
        return body(pushType, contentId, refId,
                id -> boardRepository.findByContent_Id(id).map(Board::getTitle).orElse(null),
                id -> communityCommentsRepository.findById(id).orElse(null));
    }

    private static String commentPreview(CommunityComments comment) {
        if (comment == null || comment.isDeleted() || comment.isHidden()) {
            return null;
        }
        return comment.getBody();
    }
}
