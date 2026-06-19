package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardHide;
import com.jiujitsu.api.domain.community.board.repository.BoardHideRepository;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardHideService {

    private final AuthenticationFacade authenticationFacade;
    private final BoardHideRepository boardHideRepository;
    private final BoardRepository boardRepository;

    /**
     * 게시물 숨김/숨김해제 (toggle)
     * @return true = 숨김, false = 숨김해제
     */
    public boolean toggleHide(Long contentId) {
        User user = authenticationFacade.getCurrentUser();

        Board board = boardRepository.findByContent_Id(contentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));

        if (user.equals(board.getCreatedBy())) {
            throw new ErrorException(ErrorCode.SELF_HIDE_NOT_ALLOWED);
        }

        Optional<BoardHide> existing = boardHideRepository.findByCreatedByIdAndContentId(user.getId(), contentId);

        if (existing.isPresent()) {
            boardHideRepository.delete(existing.get());
            return false;
        } else {
            boardHideRepository.save(BoardHide.builder()
                    .contentId(contentId)
                    .build());
            return true;
        }
    }

    /**
     * 현재 로그인 유저가 숨긴 게시물 content ID 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Long> getHiddenContentIds() {
        return authenticationFacade.getCurrentUserOptional()
                .map(user -> boardHideRepository.findContentIdsByUserId(user.getId()))
                .orElse(Collections.emptyList());
    }
}
