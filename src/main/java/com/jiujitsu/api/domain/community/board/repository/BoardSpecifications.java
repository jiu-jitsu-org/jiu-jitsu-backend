package com.jiujitsu.api.domain.community.board.repository;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardTag;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * 게시글 목록 조건. 검색 단어 수만큼 조건이 늘어 @Query 하나로 쓸 수 없으므로 Specification 으로 둔다
 * (Criteria API — 네이티브 SQL 없음, H2·PostgreSQL 같은 동작)
 */
public final class BoardSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private BoardSpecifications() {
    }

    // 피드와 같은 제외 조건: 숨김 글, 차단한 작성자, 내가 신고 · 숨긴 글 (IN 절이라 빈 목록 대신 sentinel 을 받는다)
    public static Specification<Board> visibleInFeed(List<Long> excludedAuthorIds, List<Long> excludedContentIds) {
        return (root, query, cb) -> cb.and(
                cb.isNull(root.get("hiddenAt")),
                cb.not(root.get("createdBy").get("id").in(excludedAuthorIds)),
                cb.not(root.get("content").get("id").in(excludedContentIds)));
    }

    /**
     * 검색: 단어마다 제목 · 본문 · 태그 중 한 곳에 들어 있는 글 (모든 단어 AND).
     * 글 쪽은 띄어쓰기 · 줄바꿈을 지우고 소문자로 비교하므로, 단어도 같은 형태(공백 없음 · 소문자)로 받는다
     */
    public static Specification<Board> containsAllWords(List<String> normalizedWords) {
        return (root, query, cb) -> cb.and(normalizedWords.stream()
                .map(word -> containsWord(root, query, cb, likePattern(word)))
                .toArray(Predicate[]::new));
    }

    private static Predicate containsWord(Root<Board> root, CriteriaQuery<?> query, CriteriaBuilder cb, String pattern) {
        Subquery<Long> tag = query.subquery(Long.class);
        Root<BoardTag> tagRoot = tag.from(BoardTag.class);
        tag.select(tagRoot.get("id")).where(
                cb.equal(tagRoot.get("board"), root),
                cb.like(normalized(cb, tagRoot.get("name")), pattern, LIKE_ESCAPE));

        return cb.or(
                cb.like(normalized(cb, root.get("title")), pattern, LIKE_ESCAPE),
                cb.like(normalized(cb, root.get("body")), pattern, LIKE_ESCAPE),
                cb.exists(tag));
    }

    // 띄어쓰기 · 줄바꿈 · 탭을 지우고 소문자로 ('오픈 매트' = '오픈매트', 'NoGi' = 'nogi')
    private static Expression<String> normalized(CriteriaBuilder cb, Expression<String> text) {
        Expression<String> result = text;
        for (String blank : List.of(" ", "\n", "\r", "\t")) {
            result = cb.function("replace", String.class, result, cb.literal(blank), cb.literal(""));
        }
        return cb.lower(result);
    }

    // 사용자가 넣은 % _ 는 글자 그대로 찾는다
    private static String likePattern(String word) {
        String escaped = word.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
