package com.jiujitsu.api.domain.community.board.repository;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.board.entity.BoardTag;
import com.jiujitsu.api.domain.community.board.service.BoardService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 글 검색 — 단어마다 제목 · 본문 · 태그, 띄어쓰기 · 대소문자 무시, 피드와 같은 제외 (#265)
@DataJpaTest
class BoardSearchRepositoryTest {

    private static final List<Long> NONE = List.of(-1L);   // 서비스와 같은 IN 절 sentinel
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 1, 12, 0);

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private TestEntityManager em;

    private User author;
    private BoardCategory category;
    private int minutes;

    @BeforeEach
    void setUp() {
        author = saveUser();
        category = em.persist(BoardCategory.builder().name("도장 · 오픈매트").isActive(true).build());
    }

    @Test
    @DisplayName("영문 대소문자 무시: 본문 nogi 글이 NOGI · nogi · NoGi 로 찾아짐")
    void caseInsensitive() {
        post("노기 오픈매트", "이번 주는 nogi 로 해요");
        post("도복 이야기", "A2L 사이즈");

        assertThat(search("NOGI")).containsExactly("노기 오픈매트");
        assertThat(search("nogi")).containsExactly("노기 오픈매트");
        assertThat(search("NoGi")).containsExactly("노기 오픈매트");
        assertThat(search("a2l")).containsExactly("도복 이야기");
    }

    @Test
    @DisplayName("띄어쓰기 무시: '오픈매트' 로 '오픈 매트' · 줄바꿈 · NBSP · 전각 공백 글도, 검색어 앞뒤 공백 무시")
    void ignoresSpaces() {
        post("토요일 오픈매트", "");
        post("일요일 오픈 매트", "");
        post("줄바꿈", "오픈\n매트 있어요");
        post("붙여넣기", "오픈\u00A0매트 · 오픈\u3000매트");

        assertThat(search("오픈매트")).containsExactly("붙여넣기", "줄바꿈", "일요일 오픈 매트", "토요일 오픈매트");
        assertThat(search(" 오픈매트 ")).containsExactly("붙여넣기", "줄바꿈", "일요일 오픈 매트", "토요일 오픈매트");
    }

    @Test
    @DisplayName("여러 단어: 모든 단어가 제목 · 본문 · 태그 어딘가에 든 글만 (단어가 서로 다른 곳에 있어도)")
    void allWords() {
        post("토요일 오픈매트", "강남역 근처", "오픈매트");
        post("수원 오픈매트", "영통", "오픈매트");
        post("강남 맛집", "주짓수 끝나고");

        assertThat(search("오픈매트 강남")).containsExactly("토요일 오픈매트");
    }

    @Test
    @DisplayName("태그에만 있는 단어로도 찾아짐")
    void tags() {
        post("손가락 아파요", "어떻게 감아요", "테이핑", "손가락");
        post("다른 글", "본문");

        assertThat(search("테이핑")).containsExactly("손가락 아파요");
    }

    @Test
    @DisplayName("% _ 는 글자 그대로 (와일드카드 아님)")
    void escapesWildcards() {
        post("할인 50%", "");
        post("할인 50퍼", "");
        post("snake_case", "");
        post("snakeXcase", "");

        assertThat(search("50%")).containsExactly("할인 50%");
        assertThat(search("e_c")).containsExactly("snake_case");
    }

    @Test
    @DisplayName("숨김 글 · 차단한 작성자 · 내가 신고 · 숨긴 글은 빠지고, 결과는 최신순")
    void exclusions() {
        Board hidden = post("오픈매트 숨김", "");
        hidden.hide();
        User blocked = saveUser();
        post("오픈매트 차단", "", blocked);
        Board reported = post("오픈매트 신고", "");
        post("오픈매트 보임1", "");
        post("오픈매트 보임2", "");
        em.flush();

        List<String> result = boardRepository.findAll(
                        BoardSpecifications.visibleInFeed(List.of(blocked.getId()), List.of(reported.getContent().getId()))
                                .and(BoardSpecifications.containsAllWords(BoardService.searchPatterns("오픈매트"))),
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(Board::getTitle).getContent();

        assertThat(result).containsExactly("오픈매트 보임2", "오픈매트 보임1");
    }

    private List<String> search(String keyword) {
        em.flush();
        em.clear();
        return boardRepository.findAll(
                        BoardSpecifications.visibleInFeed(NONE, NONE).and(BoardSpecifications.containsAllWords(BoardService.searchPatterns(keyword))),
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(Board::getTitle).getContent();
    }

    private User saveUser() {
        return em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private Board post(String title, String body, String... tags) {
        return post(title, body, author, tags);
    }

    // 부를 때마다 1분씩 늦게 쓴 글 (작성 시각을 고정해 최신순을 확정한다)
    private Board post(String title, String body, User writer, String... tags) {
        Board board = Board.builder()
                .category(category)
                .content(Content.builder().contentType(ContentType.BOARD).build())
                .title(title)
                .body(body)
                .build();
        board.addTags(Arrays.stream(tags).map(name -> BoardTag.builder().board(board).name(name).build()).toList());
        ReflectionTestUtils.setField(board, "createdBy", writer);
        em.persist(board);
        em.flush();
        em.getEntityManager().createNativeQuery("UPDATE board SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, BASE.plusMinutes(minutes++))
                .setParameter(2, board.getId())
                .executeUpdate();
        return board;
    }
}
