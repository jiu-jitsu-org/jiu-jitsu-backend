package com.jiujitsu.api.domain.community.board.mapper;

import com.jiujitsu.api.domain.community.board.dto.BoardResponse;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BoardMapperCompetitionTest {

    private final BoardMapper boardMapper = new BoardMapper();

    @Test
    @DisplayName("상세·작성·수정 응답: 대회가 비공개라 카드가 null 이어도 competitionId 는 채운다 (수정 때 다시 보내 연결 유지)")
    void response_keepsCompetitionIdWithoutCard() {
        User author = User.builder().id(1L).role(UserRole.USER).build();
        Board linked = board(Competition.builder().id(7L).published(false).build(), author);
        Board unlinked = board(null, author);

        BoardResponse detail = boardMapper.toResponse(linked, null, 0, 0, 0, false, false, false, null, true, null);
        BoardResponse written = boardMapper.toResponse(linked, author, null, null);

        assertThat(detail.competitionId()).isEqualTo(7L);
        assertThat(detail.competition()).isNull();
        assertThat(written.competitionId()).isEqualTo(7L);
        assertThat(boardMapper.toResponse(unlinked, author, null, null).competitionId()).isNull();
    }

    private Board board(Competition competition, User author) {
        Board board = Board.builder()
                .category(BoardCategory.builder().id(1L).name("대회").isActive(true).build())
                .content(Content.builder().id(100L).contentType(ContentType.BOARD).build())
                .competition(competition)
                .title("제목")
                .body("본문")
                .build();
        ReflectionTestUtils.setField(board, "createdBy", author);
        ReflectionTestUtils.setField(board, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(board, "updatedAt", LocalDateTime.now());
        return board;
    }
}
