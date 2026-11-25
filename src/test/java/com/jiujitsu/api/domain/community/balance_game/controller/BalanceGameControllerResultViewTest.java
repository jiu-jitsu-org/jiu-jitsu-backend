package com.jiujitsu.api.domain.community.balance_game.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiujitsu.api.domain.community.balance_game.service.BalanceGameService;
import com.jiujitsu.api.global.config.ApiResponseWrapperAdvice;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * '결과 봤음' 기록 API 의 경로와 응답 (#290)
 */
class BalanceGameControllerResultViewTest {

    private final BalanceGameService balanceGameService = mock(BalanceGameService.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new BalanceGameController(balanceGameService))
            .setControllerAdvice(new GlobalExceptionHandler(), new ApiResponseWrapperAdvice(new ObjectMapper()))
            .build();

    @Test
    @DisplayName("성공하면 200, data 는 null")
    void success() throws Exception {
        mockMvc.perform(post("/community/balance-game/100/result-views"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"code\":\"OK\",\"data\":null}"));
        verify(balanceGameService).markResultSeen(100L);
    }

    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"BALANCE_GAME_NOT_FOUND", "LOGIN_NOT_ACCESS"})
    @DisplayName("없는 판 · 비로그인은 해당 오류 코드로 응답한다")
    void error(ErrorCode errorCode) throws Exception {
        willThrow(new ErrorException(errorCode)).given(balanceGameService).markResultSeen(100L);

        mockMvc.perform(post("/community/balance-game/100/result-views"))
                .andExpect(status().is(errorCode.getStatus()))
                .andExpect(jsonPath("$.code").value(errorCode.getCode()));
    }
}
