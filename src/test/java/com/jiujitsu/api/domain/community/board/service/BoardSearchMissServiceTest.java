package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.AdminSearchMissListRequest;
import com.jiujitsu.api.domain.community.board.dto.BoardListRequest;
import com.jiujitsu.api.domain.community.board.entity.BoardListType;
import com.jiujitsu.api.domain.community.board.entity.BoardSearchMiss;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.board.repository.BoardSearchMissRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 결과 0건 검색어 기록 · 관리자 조회 (#266)
 */
@ExtendWith(MockitoExtension.class)
class BoardSearchMissServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private BoardSearchMissRepository boardSearchMissRepository;

    @Mock
    private BoardRepository boardRepository;

    private final Pageable first = PageRequest.of(0, 20);

    private BoardSearchMissService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 12, 28, 15, 0).atZone(SEOUL).toInstant(), SEOUL);
        service = new BoardSearchMissService(boardSearchMissRepository, boardRepository, clock);
    }

    @Test
    @DisplayName("기록: 검색 첫 페이지 0건이고 개인 제외 없이도 글이 없으면, 단어를 공백 하나로 이어 지금 시각과 저장 (회원 정보 없음)")
    @SuppressWarnings("unchecked")
    void record() {
        given(boardRepository.count(any(Specification.class))).willReturn(0L);

        service.recordIfNoPosts(search(" 베림보로  NoGi "), first, 0);

        ArgumentCaptor<BoardSearchMiss> captor = ArgumentCaptor.forClass(BoardSearchMiss.class);
        verify(boardSearchMissRepository).save(captor.capture());
        assertThat(captor.getValue().getKeyword()).isEqualTo("베림보로 nogi");
        assertThat(captor.getValue().getSearchedAt()).isEqualTo(LocalDateTime.of(2026, 12, 28, 15, 0));
    }

    @Test
    @DisplayName("기록: 검색어는 100자로 자름")
    @SuppressWarnings("unchecked")
    void record_truncated() {
        given(boardRepository.count(any(Specification.class))).willReturn(0L);

        service.recordIfNoPosts(search("가".repeat(60) + " " + "나".repeat(60)), first, 0);

        ArgumentCaptor<BoardSearchMiss> captor = ArgumentCaptor.forClass(BoardSearchMiss.class);
        verify(boardSearchMissRepository).save(captor.capture());
        assertThat(captor.getValue().getKeyword()).hasSize(BoardSearchMiss.KEYWORD_MAX_LENGTH);
    }

    @Test
    @DisplayName("기록 안 함: 결과가 있음 · 다음 페이지 · 검색이 아님 · 빈 검색어 — 다시 세지도 않음")
    void notRecorded() {
        service.recordIfNoPosts(search("오픈매트"), first, 3);
        service.recordIfNoPosts(search("베림보로"), PageRequest.of(1, 20), 0);
        service.recordIfNoPosts(new BoardListRequest(BoardListType.FEED, null, "베림보로"), first, 0);
        service.recordIfNoPosts(search("  "), first, 0);

        verifyNoInteractions(boardRepository, boardSearchMissRepository);
    }

    @Test
    @DisplayName("기록 안 함: 내 차단 · 신고 · 숨김 때문에 0건이었고 다른 사람에겐 글이 보이면")
    @SuppressWarnings("unchecked")
    void notRecorded_onlyPersonallyExcluded() {
        given(boardRepository.count(any(Specification.class))).willReturn(2L);

        service.recordIfNoPosts(search("베림보로"), first, 0);

        verify(boardSearchMissRepository, never()).save(any());
    }

    @Test
    @DisplayName("기록이 실패해도 예외를 밖으로 내지 않음 (검색 응답은 그대로)")
    @SuppressWarnings("unchecked")
    void recordFailure_swallowed() {
        given(boardRepository.count(any(Specification.class))).willReturn(0L);
        given(boardSearchMissRepository.save(any())).willThrow(new RuntimeException("db down"));

        service.recordIfNoPosts(search("베림보로"), first, 0);
    }

    private static BoardListRequest search(String keyword) {
        return new BoardListRequest(BoardListType.SEARCH, null, keyword);
    }

    @Test
    @DisplayName("조회: 기본은 오늘까지 4주(양 끝 포함) · 100개, 날짜는 그날 0시 ~ 다음 날 0시 전")
    void adminList_defaults() {
        service.getAdminList(new AdminSearchMissListRequest(null, null, null));
        service.getAdminList(new AdminSearchMissListRequest(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 7), 20));

        verify(boardSearchMissRepository).findTopKeywords(LocalDateTime.of(2026, 12, 1, 0, 0), LocalDateTime.of(2026, 12, 29, 0, 0), PageRequest.of(0, 100));
        verify(boardSearchMissRepository).findTopKeywords(LocalDateTime.of(2026, 12, 1, 0, 0), LocalDateTime.of(2026, 12, 8, 0, 0), PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("조회: from 이 to 보다 뒤면 WRONG_PARAMETER")
    void adminList_reversed() {
        assertThatThrownBy(() -> service.getAdminList(new AdminSearchMissListRequest(LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 1), null)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
        verify(boardSearchMissRepository, never()).findTopKeywords(any(), any(), any());
    }
}
