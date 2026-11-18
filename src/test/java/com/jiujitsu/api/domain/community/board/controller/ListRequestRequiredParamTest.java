package com.jiujitsu.api.domain.community.board.controller;

import com.jiujitsu.api.domain.community.board.service.BoardCategoryService;
import com.jiujitsu.api.domain.community.board.service.BoardHideService;
import com.jiujitsu.api.domain.community.board.service.BoardService;
import com.jiujitsu.api.domain.community.comment.controller.CommunityCommentsController;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 글 · 댓글 목록 조건 필수값 (#258): 없으면 서비스에서 NPE → 500 이던 것을 400 R0001 로
 */
class ListRequestRequiredParamTest {

    private final BoardService boardService = mock(BoardService.class);
    private final CommunityCommentsService commentsService = mock(CommunityCommentsService.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(
                    new BoardController(boardService, mock(BoardCategoryService.class), mock(BoardHideService.class), mock(ContentService.class)),
                    new CommunityCommentsController(commentsService))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())   // 실제 앱처럼 page · size 를 Pageable 로
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("글 목록: boardListType 이 없으면 400 R0001, 서비스는 부르지 않음")
    void boardList_requiresType() throws Exception {
        mockMvc.perform(get("/board"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.REQUIRED_PARAMETER.getCode()));
        verify(boardService, never()).getList(any(), any());

        mockMvc.perform(get("/board").param("boardListType", "FEED"))
                .andExpect(status().isOk());
        verify(boardService).getList(any(), any());
    }

    @Test
    @DisplayName("댓글 목록: 게시물 id 가 없으면 400 R0001, 서비스는 부르지 않음")
    void commentList_requiresContentId() throws Exception {
        mockMvc.perform(get("/community/comments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.REQUIRED_PARAMETER.getCode()));
        verify(commentsService, never()).getComments(any(), any());

        mockMvc.perform(get("/community/comments").param("id", "10"))
                .andExpect(status().isOk());
        verify(commentsService).getComments(any(), any());
    }
}
