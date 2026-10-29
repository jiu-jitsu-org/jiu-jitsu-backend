package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.BoardCreateRequest;
import com.jiujitsu.api.domain.community.board.dto.BoardUpdateRequest;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.factory.BoardFactory;
import com.jiujitsu.api.domain.community.board.mapper.BoardMapper;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.service.CommunityProfileService;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 게시글 응답 작성자(author)에 띠·그랄·인증 여부를 담기 위한 작성자 프로필 조회 (#163)
 */
@ExtendWith(MockitoExtension.class)
class BoardServiceAuthorProfileTest {

    @InjectMocks
    private BoardService boardService;

    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private BoardRepository boardRepository;
    @Mock private BoardFactory boardFactory;
    @Mock private BoardMapper boardMapper;
    @Mock private BoardCategoryService boardCategoryService;
    @Mock private CommunityCommentsService communityCommentsService;
    @Mock private ContentService contentService;
    @Mock private UserBlockService userBlockService;
    @Mock private NoticeService noticeService;
    @Mock private ReportService reportService;
    @Mock private BoardHideService boardHideService;
    @Mock private CommunityProfileService communityProfileService;

    @Test
    @DisplayName("목록은 페이지 작성자들의 프로필을 한 번에 조회해 각 글의 author 에 넘긴다")
    void list_fetchesAuthorProfilesOnce() {
        // given
        User me = user(1L);
        User blueAuthor = user(2L);
        User noProfileAuthor = user(3L);
        Board first = board(100L, blueAuthor);
        Board second = board(101L, noProfileAuthor);
        Board third = board(102L, blueAuthor);
        Pageable pageable = PageRequest.of(0, 20);

        CommunityProfile blueProfile = CommunityProfile.builder().user(blueAuthor).build();
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(boardRepository.findSavedBoardsNotHidden(me.getId(), pageable))
                .willReturn(new PageImpl<>(List.of(first, second, third), pageable, 3));
        given(communityProfileService.getProfilesByUserIds(Set.of(2L, 3L)))
                .willReturn(Map.of(2L, blueProfile));

        // when
        boardService.getSaveList(pageable);

        // then - 글마다가 아니라 페이지당 1번
        verify(communityProfileService, times(1)).getProfilesByUserIds(any());
        verify(boardMapper).toBoardListResponse(eq(first), eq(blueProfile), anyLong(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
        verify(boardMapper).toBoardListResponse(eq(second), isNull(), anyLong(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
        verify(boardMapper).toBoardListResponse(eq(third), eq(blueProfile), anyLong(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("상세는 작성자 프로필을 조회해 author 에 넘긴다")
    void detail_passesAuthorProfile() {
        // given
        User author = user(2L);
        Board board = board(100L, author);
        CommunityProfile profile = CommunityProfile.builder().user(author).build();
        given(boardRepository.findByContent_Id(100L)).willReturn(Optional.of(board));
        given(communityProfileService.findProfile(author)).willReturn(Optional.of(profile));

        // when
        boardService.getById(100L);

        // then
        verify(boardMapper).toResponse(eq(board), eq(profile), anyLong(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyBoolean());
    }

    @Test
    @DisplayName("작성 직후 응답은 감사용 createdBy 대신 로그인 회원과 그 프로필을 author 에 넘긴다")
    void create_passesAuthorProfile() {
        // given
        User author = user(2L);
        // 저장 직후 createdBy 는 감사(AuditorAware)가 넣은 id 만 있는 User
        Board board = board(100L, User.builder().id(2L).build());
        CommunityProfile profile = CommunityProfile.builder().user(author).build();
        given(authenticationFacade.getCurrentUser()).willReturn(author);
        given(boardFactory.createBoard(any(), any(), any(), any(), any())).willReturn(board);
        given(boardRepository.save(board)).willReturn(board);
        given(communityProfileService.findProfile(author)).willReturn(Optional.of(profile));

        // when
        boardService.create(new BoardCreateRequest(1L, "제목", "본문", null, null));

        // then
        verify(boardMapper).toResponse(board, author, profile);
    }

    @Test
    @DisplayName("수정 직후 응답도 작성자 프로필을 author 에 넘기고, 프로필이 없으면 null")
    void update_passesAuthorProfile() {
        // given
        User author = user(2L);
        Board board = board(100L, author);
        given(boardRepository.findByContent_Id(100L)).willReturn(Optional.of(board));
        given(communityProfileService.findProfile(author)).willReturn(Optional.empty());

        // when
        boardService.update(100L, new BoardUpdateRequest());

        // then
        verify(boardMapper).toResponse(board, author, null);
    }

    private User user(Long id) {
        return User.builder().id(id).role(UserRole.USER).build();
    }

    private Board board(Long contentId, User author) {
        Content content = mock(Content.class);
        lenient().when(content.getId()).thenReturn(contentId);
        Board board = mock(Board.class);
        lenient().when(board.getContent()).thenReturn(content);
        lenient().when(board.getCreatedBy()).thenReturn(author);
        return board;
    }
}
