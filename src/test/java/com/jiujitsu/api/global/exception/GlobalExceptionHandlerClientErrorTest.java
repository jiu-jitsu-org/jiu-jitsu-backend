package com.jiujitsu.api.global.exception;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring 이 요청 단계에서 던지는 오류는 500(SY002)이 아니라 알맞은 4xx (#258)
 */
class GlobalExceptionHandlerClientErrorTest {

    record Body(String name) {}

    @RestController
    static class TestController {
        @GetMapping("/test/param")
        public String param(@RequestParam("nickname") String nickname) {
            return nickname;
        }

        @GetMapping("/test/header")
        public String header(@RequestHeader("X-Os") String os) {
            return os;
        }

        @PostMapping(value = "/test/json", consumes = MediaType.APPLICATION_JSON_VALUE)
        public String json(@RequestBody Body body) {
            return body.name();
        }
    }

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(handler)
            .build();

    @Test
    @DisplayName("필수 쿼리 파라미터 · 헤더가 없으면 400 R0001")
    void missingParam_400() throws Exception {
        mockMvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.REQUIRED_PARAMETER.getCode()));
        mockMvc.perform(get("/test/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.REQUIRED_PARAMETER.getCode()));
    }

    @Test
    @DisplayName("지원하지 않는 메서드는 405 R0016, 허용 메서드는 Allow 헤더로")
    void methodNotAllowed_405() throws Exception {
        mockMvc.perform(get("/test/json"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "POST"))
                .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.getCode()));
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type 은 415 R0017")
    void unsupportedMediaType_415() throws Exception {
        mockMvc.perform(post("/test/json").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode()));
    }

    @Test
    @DisplayName("없는 주소는 404 R0015")
    void noResource_404() {
        ResponseEntity<ApiResponse<Object>> response = handler.handleServletException(
                new NoResourceFoundException(HttpMethod.GET, "no-such-path"), new MockHttpServletRequest("GET", "/api/no-such-path"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.API_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("상태를 모르는 ServletException 은 지금처럼 500 SY002")
    void otherServletException_500() {
        ResponseEntity<ApiResponse<Object>> response = handler.handleServletException(
                new ServletException("알 수 없음"), new MockHttpServletRequest("GET", "/api/x"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.SERVER_ERROR.getCode());
    }
}
