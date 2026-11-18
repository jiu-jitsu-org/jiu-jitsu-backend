package com.jiujitsu.api.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 요청 본문(JSON) 형식이 틀리면 500(SY002)이 아니라 400(R0002) (#213)
 */
class GlobalExceptionHandlerBodyTest {

    enum Region { SEOUL, BUSAN }

    record Item(Integer count) {}

    record Body(Region region, LocalDate date, Integer count, List<Item> items) {}

    @RestController
    static class TestController {
        @PostMapping("/test/body")
        public String body(@RequestBody Body body) {
            return "ok";
        }

        @PostMapping("/test/boom")
        public String boom(@RequestBody Body body) {
            throw new IllegalStateException("서버 버그");
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "{\"region\":\"NOPE\"}",                 // 없는 enum 값
            "{\"date\":\"2026/12/06\"}",             // 날짜 형식이 다름
            "{\"count\":\"열\"}",                    // 숫자 칸에 글자
            "{\"items\":[{\"count\":\"열\"}]}",      // 목록 안 칸
            "{\"region\":",                          // 깨진 JSON
            ""                                       // 본문 없음
    })
    @DisplayName("본문을 읽지 못하면 400 R0002")
    void unreadableBody_400(String json) throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.WRONG_PARAMETER.getCode()));
    }

    @Test
    @DisplayName("맞는 본문은 그대로 통과, 진짜 서버 오류는 그대로 500 SY002")
    void validBody_and_serverError() throws Exception {
        String json = "{\"region\":\"SEOUL\",\"date\":\"2026-12-06\",\"count\":3,\"items\":[{\"count\":1}]}";
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
        mockMvc.perform(post("/test/boom").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.SERVER_ERROR.getCode()));
    }
}
