package com.jiujitsu.api.domain.community.board.controller;

import com.jiujitsu.api.domain.community.board.dto.AdminSearchMissListRequest;
import com.jiujitsu.api.domain.community.board.dto.AdminSearchMissResponse;
import com.jiujitsu.api.domain.community.board.service.BoardSearchMissService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.security.annotation.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "admin-board-search-miss-controller", description = "관리자 결과 0건 검색어 API")
@CommonApiResponses
@AdminOnly
@RestController
@RequestMapping("/admin/board/search-misses")
@RequiredArgsConstructor
public class AdminBoardSearchMissController {

    private final BoardSearchMissService boardSearchMissService;

    @Operation(summary = "결과 0건 검색어",
            description = "기간 안에 결과가 0건이었던 검색어를 많이 나온 순(같으면 최근 순)으로 반환합니다. "
                    + "검색어는 단어를 소문자 · 공백 하나로 이은 값이라 'NoGi' 와 'nogi' 는 같이 셉니다. 검색 첫 페이지만 세고, 누가 찾았는지는 남기지 않습니다. "
                    + "기간 기본은 오늘까지 4주, limit 기본 100 · 최대 500. (ROLE_ADMIN 필요)")
    @ApiErrorCodeExamples({ErrorCode.WRONG_PARAMETER})
    @GetMapping
    public List<AdminSearchMissResponse> getSearchMisses(@Valid @ParameterObject AdminSearchMissListRequest request) {
        return boardSearchMissService.getAdminList(request);
    }
}
