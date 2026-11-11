package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 커뮤니티 알림의 미리보기 본문 — 알림함 카드 body 와 푸시 본문이 같은 규칙을 쓰게 한 곳에 둔다 (#196)
 * - 내 게시글에 댓글 · 좋아요 : 게시글 제목 (밸런스 게임은 게시글이 없어 null)
 * - 내 댓글에 대댓글          : 대댓글이 달린 내 댓글
 * - 내 댓글에 좋아요          : 좋아요 받은 댓글
 * - 댓글이 삭제·숨김(신고 포함)이면 null — 지운 글이 알림함·잠금화면에 다시 보이지 않게
 */
@Component
@RequiredArgsConstructor
public class NoticePreview {

    private final BoardRepository boardRepository;
    private final CommunityCommentsRepository communityCommentsRepository;

    /** 댓글 미리보기. 삭제·숨김이면 null (알림함 목록의 일괄 계산도 이걸 쓴다) */
    public static String commentPreview(CommunityComments comment) {
        if (comment == null || comment.isDeleted() || comment.isHidden()) {
            return null;
        }
        return comment.getBody();
    }

    /**
     * 알림 하나의 미리보기 — 푸시를 보낼 때 쓴다 (알림함 목록은 NoticeService 가 화면 단위로 일괄 조회)
     *
     * @param contentId 게시글·밸런스 게임 contentId
     * @param refId     NEW_CHILD_COMMENTS = 새 대댓글, COMMENTS_LIKE = 좋아요 받은 댓글
     */
    public String of(FcmPushType pushType, Long contentId, Long refId) {
        return switch (pushType) {
            case NEW_COMMENTS, CONTENTS_LIKE -> contentId == null ? null
                    : boardRepository.findByContent_Id(contentId).map(Board::getTitle).orElse(null);
            case NEW_CHILD_COMMENTS -> refId == null ? null
                    : communityCommentsRepository.findById(refId)
                            .map(CommunityComments::getParentId)
                            .filter(parentId -> parentId != 0)
                            .flatMap(communityCommentsRepository::findById)
                            .map(NoticePreview::commentPreview)
                            .orElse(null);
            case COMMENTS_LIKE -> refId == null ? null
                    : communityCommentsRepository.findById(refId).map(NoticePreview::commentPreview).orElse(null);
            default -> null;
        };
    }
}
