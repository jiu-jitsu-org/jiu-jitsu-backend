package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.AdminSearchMissListRequest;
import com.jiujitsu.api.domain.community.board.dto.AdminSearchMissResponse;
import com.jiujitsu.api.domain.community.board.entity.BoardSearchMiss;
import com.jiujitsu.api.domain.community.board.repository.BoardSearchMissRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 결과 0건 검색어 기록 · 관리자 조회 (#266). 검색어 원문은 분석 도구(GA)로 보내지 않기로 해서 서버가 회원 정보 없이 모은다
 */
@Service
@RequiredArgsConstructor
public class BoardSearchMissService {

    private static final int DEFAULT_DAYS = 28;
    private static final int DEFAULT_LIMIT = 100;

    private final BoardSearchMissRepository boardSearchMissRepository;
    private final Clock clock;

    /**
     * 검색 단어로 0건 기록. 검색 조회(readOnly) 밖의 새 트랜잭션에서 쓴다.
     * 실패는 부르는 쪽에서 잡는다 — 여기서 잡으면 이 트랜잭션이 rollback-only 로 남아 커밋 때 예외가 다시 난다
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(List<String> words) {
        String keyword = String.join(" ", words);
        if (keyword.isEmpty()) {
            return;
        }
        boardSearchMissRepository.save(BoardSearchMiss.builder()
                .keyword(keyword.length() > BoardSearchMiss.KEYWORD_MAX_LENGTH ? keyword.substring(0, BoardSearchMiss.KEYWORD_MAX_LENGTH) : keyword)
                .searchedAt(LocalDateTime.now(clock))
                .build());
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
