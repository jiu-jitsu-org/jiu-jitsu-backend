package com.jiujitsu.api.global.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiujitsu.api.global.exception.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
@RequiredArgsConstructor
public class ApiResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Swagger 요청은 무시 (springdoc-openapi 관련)
        String className = returnType.getDeclaringClass().getName();
        if (className.startsWith("org.springdoc") || className.startsWith("org.springframework.boot.actuate")) {
            return false;
        }

        // ApiResponse 나 ResponseEntity<ApiResponse>면 가공하지 않음
        return !returnType.getParameterType().equals(ApiResponse.class);
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            org.springframework.http.server.ServerHttpRequest request,
            org.springframework.http.server.ServerHttpResponse response) {

        // 에러는 핸들러 따로 있음 / ResponseEntity는 그대로 리턴
        if (body instanceof ApiResponse || body instanceof ResponseEntity) {
            return body;
        }

//        // String 추가 로직 처리
//        if (data instanceof String) {
//            return ResponseEntity.ok()
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .data(ApiResponse.success(data));
//        }
        if (body instanceof String) {
            // 여기서 JSON 문자열로 직접 변환
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                return objectMapper.writeValueAsString(ApiResponse.success(body));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return ApiResponse.success(body);
    }
}