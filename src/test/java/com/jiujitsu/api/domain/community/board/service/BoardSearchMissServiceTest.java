package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.AdminSearchMissListRequest;
import com.jiujitsu.api.domain.community.board.entity.BoardSearchMiss;
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

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 결과 0건 검색어 기록 · 관리자 조회 (#266)
 */
@ExtendWith(MockitoExtension.class)
class BoardSearchMissServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private BoardSearchMissRepository boardSearchMissRepository;

    private BoardSearchMissService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 12, 28, 15, 0).atZone(SEOUL).toInstant(), SEOUL);
        service = new BoardSearchMissService(boardSearchMissRepository, clock);
    }

    @Test
    @DisplayName("기록: 단어를 공백 하나로 이어 지금 시각과 저장, 회원 정보 없음, 100자로 자름")
    void record() {
        service.record(List.of("베림보로", "nogi"));
        service.record(List.of("가".repeat(60), "나".repeat(60)));

        ArgumentCaptor<BoardSearchMiss> captor = ArgumentCaptor.forClass(BoardSearchMiss.class);
        verify(boardSearchMissRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getKeyword()).isEqualTo("베림보로 nogi");
        assertThat(captor.getAllValues().get(0).getSearchedAt()).isEqualTo(LocalDateTime.of(2026, 12, 28, 15, 0));
        assertThat(captor.getAllValues().get(1).getKeyword()).hasSize(BoardSearchMiss.KEYWORD_MAX_LENGTH);
    }

    @Test
    @DisplayName("기록: 단어가 없으면 저장하지 않음")
    void record_empty() {
        service.record(List.of());

        verifyNoInteractions(boardSearchMissRepository);
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
