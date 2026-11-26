package com.jiujitsu.api.domain.community.balance_game.controller;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceResultNoticeRunResponse;
import com.jiujitsu.api.domain.community.balance_game.service.BalanceResultNoticeRunService;
import com.jiujitsu.api.global.security.annotation.AdminOnly;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 결과 알림 관리자 재실행 (#291)
 */
class AdminBalanceResultNoticeControllerTest {

    private final BalanceResultNoticeRunService runService = mock(BalanceResultNoticeRunService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminBalanceResultNoticeController(runService)).build();

    @Test
    @DisplayName("지금 보내고 대상 판 수 · 보낸 수 · 실패 수를 준다")
    void run() throws Exception {
        given(runService.runYesterday()).willReturn(new BalanceResultNoticeRunResponse(LocalDate.of(2026, 11, 25), 2, 12, 1));

        mockMvc.perform(post("/admin/balance-game/result-notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games").value(2))
                .andExpect(jsonPath("$.sent").value(12))
                .andExpect(jsonPath("$.failed").value(1));
    }

    @Test
    @DisplayName("관리자만 부를 수 있다")
    void adminOnly() {
        assertThat(AdminBalanceResultNoticeController.class).hasAnnotation(AdminOnly.class);
    }
}
