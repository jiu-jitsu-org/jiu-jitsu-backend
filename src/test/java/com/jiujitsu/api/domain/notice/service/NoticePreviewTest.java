package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

// 알림 미리보기 본문 (#196): 알림함 카드 body 와 푸시 본문이 같은 규칙
class NoticePreviewTest {

    private static final Long CONTENT = 10L;

    private final BoardRepository boardRepository = mock(BoardRepository.class);
    private final CommunityCommentsRepository commentsRepository = mock(CommunityCommentsRepository.class);
    private final NoticePreview preview = new NoticePreview(boardRepository, commentsRepository);

    private CommunityComments comment(long id, long parentId, String body, String state) {
        CommunityComments c = mock(CommunityComments.class);
        lenient().when(c.getId()).thenReturn(id);
        lenient().when(c.getParentId()).thenReturn(parentId);
        lenient().when(c.getBody()).thenReturn(body);
        lenient().when(c.isDeleted()).thenReturn("삭제".equals(state));
        lenient().when(c.isHidden()).thenReturn("숨김".equals(state));
        given(commentsRepository.findById(id)).willReturn(Optional.of(c));
        return c;
    }

    @Test
    @DisplayName("내 게시글에 댓글 · 좋아요 = 게시글 제목, 게시글이 없으면(밸런스 게임) null")
    void boardTitle() {
        Board board = mock(Board.class);
        given(board.getTitle()).willReturn("오늘 오픈매트 후기");
        given(boardRepository.findByContent_Id(CONTENT)).willReturn(Optional.of(board));

        assertThat(preview.of(FcmPushType.NEW_COMMENTS, CONTENT, 100L)).isEqualTo("오늘 오픈매트 후기");
        assertThat(preview.of(FcmPushType.CONTENTS_LIKE, CONTENT, null)).isEqualTo("오늘 오픈매트 후기");
        assertThat(preview.of(FcmPushType.CONTENTS_LIKE, 99L, null)).isNull();
    }

    @Test
    @DisplayName("내 댓글에 대댓글 = 대댓글이 달린 내 댓글 (refId 는 새 대댓글)")
    void reply_parentComment() {
        comment(1L, 0L, "저도 그 기술 배워보고 싶어요", "정상");
        comment(101L, 1L, "저도 동의해요", "정상");

        assertThat(preview.of(FcmPushType.NEW_CHILD_COMMENTS, CONTENT, 101L)).isEqualTo("저도 그 기술 배워보고 싶어요");
    }

    @ParameterizedTest(name = "부모 댓글 {0}")
    @ValueSource(strings = {"삭제", "숨김"})
    @DisplayName("대댓글: 부모 댓글이 삭제·숨김(신고 포함)이면 null — 지운 글이 잠금화면·알림함에 다시 뜨지 않게")
    void reply_deletedOrHiddenParent_null(String state) {
        comment(1L, 0L, "지운 댓글 원문", state);
        comment(101L, 1L, "저도 동의해요", "정상");

        assertThat(preview.of(FcmPushType.NEW_CHILD_COMMENTS, CONTENT, 101L)).isNull();
    }

    @Test
    @DisplayName("내 댓글에 좋아요 = 좋아요 받은 댓글")
    void commentLike_comment() {
        comment(1L, 0L, "저도 그 기술 배워보고 싶어요", "정상");

        assertThat(preview.of(FcmPushType.COMMENTS_LIKE, CONTENT, 1L)).isEqualTo("저도 그 기술 배워보고 싶어요");
    }

    @ParameterizedTest(name = "댓글 {0}")
    @ValueSource(strings = {"삭제", "숨김"})
    @DisplayName("댓글 좋아요: 댓글이 삭제·숨김이면 null")
    void commentLike_deletedOrHidden_null(String state) {
        comment(1L, 0L, "지운 댓글 원문", state);

        assertThat(preview.of(FcmPushType.COMMENTS_LIKE, CONTENT, 1L)).isNull();
    }

    @Test
    @DisplayName("대상 댓글이 없으면 null, 대회 알림은 미리보기 대상이 아님")
    void missing_or_other_null() {
        assertThat(preview.of(FcmPushType.COMMENTS_LIKE, CONTENT, 999L)).isNull();
        assertThat(preview.of(FcmPushType.NEW_CHILD_COMMENTS, CONTENT, null)).isNull();
        assertThat(preview.of(FcmPushType.COMPETITION_EARLY_BIRD_DEADLINE, 1L, null)).isNull();
    }

    @Test
    @DisplayName("대댓글 알림인데 refId 가 최상위 댓글(parentId 0)이면 null — id 0 을 부모로 조회하지 않음, contentId 가 없으면 게시글 제목도 null")
    void rootComment_or_noContent_null() {
        comment(1L, 0L, "최상위 댓글", "정상");

        assertThat(preview.of(FcmPushType.NEW_CHILD_COMMENTS, CONTENT, 1L)).isNull();
        verify(commentsRepository, never()).findById(0L);
        assertThat(preview.of(FcmPushType.NEW_COMMENTS, null, null)).isNull();
        assertThat(preview.of(FcmPushType.CONTENTS_LIKE, null, null)).isNull();
        verifyNoInteractions(boardRepository);
    }
}
