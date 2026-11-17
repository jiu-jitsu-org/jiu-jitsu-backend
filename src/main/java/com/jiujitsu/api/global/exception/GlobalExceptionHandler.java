package com.jiujitsu.api.global.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ErrorException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException(ErrorException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();

        if (errorCode.getStatus() >= 500) {
            log.error("서버 오류: {}", e.getMessage(), e);
        } else {
            log.warn("클라이언트 오류: {}", e.getMessage());
        }

        request.setAttribute("api.error.type", e.getClass().getSimpleName());
        request.setAttribute("api.error.message", errorCode.getMessage());

        return ResponseEntity
                .status(HttpStatus.valueOf(errorCode.getStatus()))
                .body(ApiResponse.error(errorCode));
    }

    /**
     * 쿼리·경로 파라미터 타입 불일치 (예: enum 에 없는 값) → 500 대신 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        request.setAttribute("api.error.type", e.getClass().getSimpleName());
        request.setAttribute("api.error.message", "Wrong parameter: " + e.getName());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.WRONG_PARAMETER));
    }

    /**
     * 요청 본문(JSON)을 읽지 못함 (#213) → 500 대신 400
     * - enum 에 없는 값, 날짜 · 숫자 형식이 다른 값, 깨진 JSON, 본문이 없음
     * - 클라이언트가 고칠 수 있는 오류라 error 가 아니라 warn 으로 남긴다. 어느 칸이 틀렸는지는 로그에만
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        String field = e.getCause() instanceof JsonMappingException jme ? fieldPath(jme) : null;
        String detail = field == null ? "Unreadable request body" : "Unreadable request body: " + field;

        log.warn("Request body not readable - {} {}: {}", request.getMethod(), request.getRequestURI(), detail);
        request.setAttribute("api.error.type", e.getClass().getSimpleName());
        request.setAttribute("api.error.message", detail);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.WRONG_PARAMETER));
    }

    // Jackson 오류 위치를 칸 이름으로 (예: fees[0].label). 위치가 없으면(깨진 JSON 등) null
    private static String fieldPath(JsonMappingException e) {
        StringBuilder path = new StringBuilder();
        for (JsonMappingException.Reference ref : e.getPath()) {
            if (ref.getFieldName() != null) {
                if (!path.isEmpty()) path.append('.');
                path.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                path.append('[').append(ref.getIndex()).append(']');
            }
        }
        return path.isEmpty() ? null : path.toString();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("Runtime exception occurred", e);
        request.setAttribute("api.error.type", e.getClass().getSimpleName());
        request.setAttribute("api.error.message", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.SERVER_ERROR));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Object>> handleValidationException(Exception e, HttpServletRequest request) {
        BindingResult bindingResult = e instanceof MethodArgumentNotValidException mave
                ? mave.getBindingResult()
                : ((BindException) e).getBindingResult();

        FieldError fieldError = bindingResult.getFieldErrors().stream().findFirst().orElse(null);

        if (fieldError == null) {
            request.setAttribute("api.error.type", e.getClass().getSimpleName());
            request.setAttribute("api.error.message", "Required/wrong parameter");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ErrorCode.WRONG_PARAMETER));
        }

        ErrorCode errorCode = resolveValidationErrorCode(fieldError);
        String message = fieldError.getDefaultMessage();

        log.warn("Validation exception - field: {}, message: {}", fieldError.getField(), message);
        request.setAttribute("api.error.type", e.getClass().getSimpleName());
        request.setAttribute("api.error.message", fieldError.getField() + ": " + message);

        return ResponseEntity
                .status(HttpStatus.valueOf(errorCode.getStatus()))
                .body(new ApiResponse<>(false, errorCode.getCode(), message, null));
    }

    private static final Set<String> REQUIRED_ANNOTATION_TYPES = Set.of("NotBlank", "NotNull", "NotEmpty");

    private ErrorCode resolveValidationErrorCode(FieldError fieldError) {
        String[] codes = fieldError.getCodes();
        if (codes == null || codes.length == 0) {
            return ErrorCode.WRONG_PARAMETER;
        }
        // getCodes() 배열의 마지막 요소가 어노테이션 단순명 (e.g. "NotBlank", "Email", "Size")
        String annotationType = codes[codes.length - 1];
        return REQUIRED_ANNOTATION_TYPES.contains(annotationType)
                ? ErrorCode.REQUIRED_PARAMETER
                : ErrorCode.WRONG_PARAMETER;
    }

    /**
     * Spring 이 요청 단계에서 던지는 오류 (#258) → 500 대신 알맞은 4xx
     * - 필수 쿼리 파라미터 · 헤더 누락 400 R0001, 없는 주소 404 R0015, 지원하지 않는 메서드 405 R0016, Content-Type 415 R0017
     * - 그 밖의 4xx 는 상태 그대로 R0002. 5xx(예: 경로 변수 매핑 누락 = 서버 버그)는 지금처럼 500 SY002
     * - 405 의 Allow 처럼 Spring 이 정한 응답 헤더는 그대로 넘긴다
     */
    @ExceptionHandler(ServletException.class)
    public ResponseEntity<ApiResponse<Object>> handleServletException(ServletException e, HttpServletRequest request) {
        if (!(e instanceof ErrorResponse errorResponse) || !errorResponse.getStatusCode().is4xxClientError()) {
            return handleGenericException(e, request);
        }

        HttpStatusCode status = errorResponse.getStatusCode();
        ErrorCode errorCode = resolveClientErrorCode(e, status);

        log.warn("Client request error - {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        request.setAttribute("api.error.type", e.getClass().getSimpleName());
        request.setAttribute("api.error.message", e.getMessage());

        return ResponseEntity
                .status(status)
                .headers(errorResponse.getHeaders())
                .body(new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null));
    }

    private static ErrorCode resolveClientErrorCode(ServletException e, HttpStatusCode status) {
        if (e instanceof MissingRequestValueException) {
            return ErrorCode.REQUIRED_PARAMETER;
        }
        return switch (status.value()) {
            case 404 -> ErrorCode.API_NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> ErrorCode.WRONG_PARAMETER;
        };
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception e, HttpServletRequest request) {
        log.error("Unexpected exception occurred", e);
        request.setAttribute("api.error.type", e.getClass().getSimpleName());
        request.setAttribute("api.error.message", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.SERVER_ERROR));
    }
}
