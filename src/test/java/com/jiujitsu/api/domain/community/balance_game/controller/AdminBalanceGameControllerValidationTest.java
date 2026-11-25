package com.jiujitsu.api.domain.community.balance_game.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameCreateRequest;
import com.jiujitsu.api.domain.community.balance_game.service.BalanceGameService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 등록 요청 검증 — 타이틀 필수 · 30자, 선택지 100자 (#289)
 */
class AdminBalanceGameControllerValidationTest {

    private final BalanceGameService balanceGameService = mock(BalanceGameService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminBalanceGameController(balanceGameService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    private ResultActions create(String title, String optionAText, String optionBText) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("optionAText", optionAText);
        body.put("optionBText", optionBText);
        return mockMvc.perform(post("/admin/balance-game")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", "   ", "　　"})     // 누락, 빈 값, 공백만, 전각 공백만
    @DisplayName("타이틀이 없거나 공백뿐이면 400 R0001")
    void titleRequired(String title) throws Exception {
        create(title, "옵션A", "옵션B")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.REQUIRED_PARAMETER.getCode()));
        verify(balanceGameService, never()).create(any());
    }

    static Stream<List<String>> overLimit() {
        return Stream.of(
                List.of("가".repeat(31), "옵션A", "옵션B"),
                List.of("타이틀", "가".repeat(101), "옵션B"),
                List.of("타이틀", "옵션A", "가".repeat(101)));
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("overLimit")
    @DisplayName("타이틀 31자, 선택지 101자는 400 R0002")
    void overLimit(List<String> texts) throws Exception {
        create(texts.get(0), texts.get(1), texts.get(2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.WRONG_PARAMETER.getCode()));
        verify(balanceGameService, never()).create(any());
    }

    @ParameterizedTest(name = "[{index}]")
    @ValueSource(strings = {"", "  ", "　"})
    @DisplayName("타이틀 30자 · 선택지 100자까지 통과, 길이는 앞뒤 공백을 자른 값으로 센다")
    void atLimit(String space) throws Exception {
        String title = "가".repeat(30);

        create(space + title + space, "가".repeat(100), "나".repeat(100)).andExpect(status().isOk());

        ArgumentCaptor<BalanceGameCreateRequest> captor = ArgumentCaptor.forClass(BalanceGameCreateRequest.class);
        verify(balanceGameService).create(captor.capture());
        assertThat(captor.getValue().title()).isEqualTo(title);
    }
}
