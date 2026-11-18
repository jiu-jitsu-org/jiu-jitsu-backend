package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.BoardCategoryResponse;
import com.jiujitsu.api.domain.community.board.dto.BoardCategoryWeeklyTopicRequest;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.board.repository.BoardCategoryRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 커뮤니티 카테고리 6개 — 이름 · 설명 · 순서 (#263), 이번 주 써 볼 주제 (#267)
 */
@ExtendWith(MockitoExtension.class)
class BoardCategoryServiceTest {

    @InjectMocks
    private BoardCategoryService boardCategoryService;

    @Mock
    private BoardCategoryRepository boardCategoryRepository;

    @Test
    @DisplayName("목록: 리포지토리 순서 그대로, 이름 · 설명을 담는다")
    void getCategory() {
        given(boardCategoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc()).willReturn(List.of(
                BoardCategory.builder().id(3L).name("도장 · 오픈매트").description("도장 고르기, 오픈매트, 승급 · 도장 생활").displayOrder(1).isActive(true).build(),
                BoardCategory.builder().id(5L).name("대회").description("대회 준비, 대회 후기").displayOrder(2).isActive(true).build()));

        List<BoardCategoryResponse> result = boardCategoryService.getCategory();

        assertThat(result).extracting(BoardCategoryResponse::getId).containsExactly(3L, 5L);
        assertThat(result).extracting(BoardCategoryResponse::getCategoryName).containsExactly("도장 · 오픈매트", "대회");
        assertThat(result).extracting(BoardCategoryResponse::getDescription).containsExactly("도장 고르기, 오픈매트, 승급 · 도장 생활", "대회 준비, 대회 후기");
    }

    @Test
    @DisplayName("기본 카테고리 생성: 6개를 순서 · 설명과 함께, 이미 있는 이름은 건너뜀")
    @SuppressWarnings("unchecked")
    void createCategory() {
        given(boardCategoryRepository.existsByName(anyString())).willAnswer(inv -> "대회".equals(inv.getArgument(0)));

        boardCategoryService.createCategory();

        ArgumentCaptor<List<BoardCategory>> captor = ArgumentCaptor.forClass(List.class);
        verify(boardCategoryRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(BoardCategory::getName)
                .containsExactly("도장 · 오픈매트", "훈련 & 기술", "도복 · 장비", "몸관리 · 부상", "매트 위 수다");
        assertThat(captor.getValue()).extracting(BoardCategory::getDisplayOrder).containsExactly(1, 3, 4, 5, 6);
        assertThat(captor.getValue()).allSatisfy(c -> assertThat(c.getDescription()).isNotBlank());
    }

    @Test
    @DisplayName("써 볼 주제: 넣으면 앞뒤 공백을 떼고 응답에 나가고, null · 빈 값 · 공백만이면 지움")
    void changeWeeklyTopic() {
        BoardCategory body = BoardCategory.builder().id(5L).name("몸관리 · 부상").description("부상 · 회복, 체력 · 체중 관리").displayOrder(5).isActive(true).build();
        given(boardCategoryRepository.findById(5L)).willReturn(Optional.of(body));

        BoardCategoryResponse set = boardCategoryService.changeWeeklyTopic(5L, new BoardCategoryWeeklyTopicRequest("  스파링하다 귀가 부었을 때, 어떻게 하셨나요? "));
        assertThat(set.getWeeklyTopic()).isEqualTo("스파링하다 귀가 부었을 때, 어떻게 하셨나요?");
        assertThat(body.getWeeklyTopic()).isEqualTo("스파링하다 귀가 부었을 때, 어떻게 하셨나요?");

        assertThat(boardCategoryService.changeWeeklyTopic(5L, new BoardCategoryWeeklyTopicRequest("   ")).getWeeklyTopic()).isNull();
        boardCategoryService.changeWeeklyTopic(5L, new BoardCategoryWeeklyTopicRequest("다시"));
        assertThat(boardCategoryService.changeWeeklyTopic(5L, new BoardCategoryWeeklyTopicRequest(null)).getWeeklyTopic()).isNull();
        boardCategoryService.changeWeeklyTopic(5L, new BoardCategoryWeeklyTopicRequest("다시"));
        assertThat(boardCategoryService.changeWeeklyTopic(5L, null).getWeeklyTopic()).isNull();
    }

    @Test
    @DisplayName("써 볼 주제: 없는 카테고리는 BOARD_CATEGORY_NOT_FOUND")
    void changeWeeklyTopic_notFound() {
        given(boardCategoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> boardCategoryService.changeWeeklyTopic(99L, new BoardCategoryWeeklyTopicRequest("주제")))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOARD_CATEGORY_NOT_FOUND);
    }
}
