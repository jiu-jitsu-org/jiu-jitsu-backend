package com.jiujitsu.api.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ErrorException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException(ErrorException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("예외 발생: {}", e.getMessage(), e);

        return ResponseEntity
                .status(HttpStatus.valueOf(errorCode.getStatus()))
                .body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException e) {
        log.error("Runtime exception occurred", e);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.SERVER_ERROR));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Object>> handleValidationException(Exception e) {
        BindingResult bindingResult = e instanceof MethodArgumentNotValidException mave
                ? mave.getBindingResult()
                : ((BindException) e).getBindingResult();

        FieldError fieldError = bindingResult.getFieldErrors().stream().findFirst().orElse(null);

        if (fieldError == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ErrorCode.WRONG_PARAMETER));
        }

        ErrorCode errorCode = resolveValidationErrorCode(fieldError);
        String message = fieldError.getDefaultMessage();

        log.error("Validation exception - field: {}, message: {}", fieldError.getField(), message);

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception e) {
        log.error("Unexpected exception occurred", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.SERVER_ERROR));
    }
}
