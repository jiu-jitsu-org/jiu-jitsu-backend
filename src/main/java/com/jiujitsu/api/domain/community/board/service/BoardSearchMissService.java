package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.AdminSearchMissListRequest;
import com.jiujitsu.api.domain.community.board.dto.AdminSearchMissResponse;
import com.jiujitsu.api.domain.community.board.dto.BoardListRequest;
import com.jiujitsu.api.domain.community.board.entity.BoardListType;
import com.jiujitsu.api.domain.community.board.entity.BoardSearchMiss;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.board.repository.BoardSearchMissRepository;
import com.jiujitsu.api.domain.community.board.repository.BoardSpecifications;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 결과 0건 검색어 기록 · 관리자 조회 (#266). 검색어 원문은 분석 도구(GA)로 보내지 않기로 해서 서버가 회원 정보 없이 모은다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoardSearchMissService {

    private static final int DEFAULT_DAYS = 28;
    private static final int DEFAULT_LIMIT = 100;
    private static final List<Long> NONE = List.of(-1L);   // IN 절 sentinel — 개인 제외(차단 · 신고 · 숨김) 없이 센다

    private final BoardSearchMissRepository boardSearchMissRepository;
    private final BoardRepository boardRepository;
    private final Clock clock;

    /**
     * 검색 응답이 끝난 뒤(컨트롤러) 부른다 — 검색 트랜잭션의 커넥션을 쥔 채 두 번째 커넥션을 잡지 않게.
     * 첫 페이지가 0건일 때만 (다음 페이지 요청으로 두 번 세지 않게), 그리고 내 차단 · 신고 · 숨김을 빼고도 글이 없을 때만 남긴다
     * (기록 목적이 '글이 없는 주제'라서). 트랜잭션 없이 리포지토리 호출마다 짧게 끝내고, 실패해도 검색 응답은 막지 않는다
     */
    public void recordIfNoPosts(BoardListRequest request, Pageable pageable, long totalElements) {
        if (request.boardListType() != BoardListType.SEARCH || pageable.getPageNumber() != 0 || totalElements > 0) {
            return;
        }
        List<String> words = BoardService.searchWords(request.searchKeyword());
        if (words.isEmpty()) {
            return;
        }
        try {
            long withoutPersonalExclusions = boardRepository.count(BoardSpecifications.visibleInFeed(NONE, NONE)
                    .and(BoardSpecifications.containsAllWords(BoardService.toLikePatterns(words))));
            if (withoutPersonalExclusions > 0) {
                return;
            }
            String keyword = String.join(" ", words);
            boardSearchMissRepository.save(BoardSearchMiss.builder()
                    .keyword(keyword.length() > BoardSearchMiss.KEYWORD_MAX_LENGTH ? keyword.substring(0, BoardSearchMiss.KEYWORD_MAX_LENGTH) : keyword)
                    .searchedAt(LocalDateTime.now(clock))
                    .build());
        } catch (RuntimeException e) {
            log.warn("0건 검색어 기록 실패", e);
        }
    }

    /**
     * 기간 안의 0건 검색어를 많이 나온 순으로 (관리자). 기간은 날짜 단위, 양 끝 포함
     */
    @Transactional(readOnly = true)
    public List<AdminSearchMissResponse> getAdminList(AdminSearchMissListRequest request) {
        LocalDate to = request.to() != null ? request.to() : LocalDate.now(clock);
        LocalDate from = request.from() != null ? request.from() : to.minusDays(DEFAULT_DAYS - 1);
        if (from.isAfter(to)) {
            throw new ErrorException(ErrorCode.WRONG_PARAMETER);
        }
        int limit = request.limit() != null ? request.limit() : DEFAULT_LIMIT;
        return boardSearchMissRepository.findTopKeywords(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), PageRequest.of(0, limit));
    }
}
