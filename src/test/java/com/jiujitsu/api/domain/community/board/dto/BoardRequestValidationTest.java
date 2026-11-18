package com.jiujitsu.api.domain.community.board.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BoardRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static BoardCreateRequest create(String title, String body) {
        return new BoardCreateRequest(1L, title, body, null, null, null);
    }

    private static BoardUpdateRequest update(String title, String body) {
        return new BoardUpdateRequest(null, title, body, null, null, null);
    }

    private Set<String> invalidFields(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    /** GlobalExceptionHandler 는 @Size 실패를 400 WRONG_PARAMETER 로, 필수 실패를 REQUIRED_PARAMETER 로 응답 */
    private Set<String> violatedAnnotations(Object request) {
        return validator.validate(request).stream()
                .map(v -> v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("등록: 제목 45자 · 본문 3,000자까지 통과")
    void createAtLimit() {
        assertThat(validator.validate(create("가".repeat(45), "가".repeat(3_000)))).isEmpty();
    }

    @Test
    @DisplayName("등록: 제목 46자 · 본문 3,001자는 길이 오류 (400 WRONG_PARAMETER)")
    void createOverLimit() {
        assertThat(invalidFields(create("가".repeat(46), "가".repeat(3_001)))).containsExactlyInAnyOrder("title", "body");
        assertThat(violatedAnnotations(create("가".repeat(46), "가".repeat(3_001)))).containsExactly("Size");
    }

    @Test
    @DisplayName("등록: 제목은 필수, 내용은 선택 — 생략 · 빈 값 · 공백만 통과 (#264)")
    void createRequired() {
        assertThat(invalidFields(create(" ", "본문"))).containsExactly("title");
        assertThat(violatedAnnotations(create(" ", "본문"))).containsExactly("NotBlank");
        assertThat(validator.validate(create("제목", null))).isEmpty();
        assertThat(validator.validate(create("제목", ""))).isEmpty();
        assertThat(validator.validate(create("제목", "  "))).isEmpty();
    }

    @Test
    @DisplayName("수정: 등록과 같은 상한, 보내지 않은 칸은 검사하지 않음")
    void update() {
        assertThat(validator.validate(update("가".repeat(45), "가".repeat(3_000)))).isEmpty();
        assertThat(validator.validate(update(null, null))).isEmpty();
        assertThat(invalidFields(update("가".repeat(46), null))).containsExactly("title");
        assertThat(invalidFields(update(null, "가".repeat(3_001)))).containsExactly("body");
    }

    @Test
    @DisplayName("글자 수는 웹 maxLength 와 같은 UTF-16 단위 — 이모지 하나는 2자")
    void utf16() {
        String emoji = "🥋"; // UTF-16 2자
        assertThat(validator.validate(create(emoji.repeat(22) + "가", "본문"))).isEmpty();      // 45
        assertThat(invalidFields(create(emoji.repeat(23), "본문"))).containsExactly("title");  // 46
    }
}
