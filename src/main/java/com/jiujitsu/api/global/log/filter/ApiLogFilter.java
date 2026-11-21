package com.jiujitsu.api.global.log.filter;

import com.jiujitsu.api.global.log.entity.ApiLog;
import com.jiujitsu.api.global.log.service.ApiLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiLogFilter extends OncePerRequestFilter {

    private final ApiLogService apiLogService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        LocalDateTime requestedAt = LocalDateTime.now();
        try {
            filterChain.doFilter(request, response);
        } finally {
            saveLog(request, response, requestedAt);
        }
    }

    private void saveLog(HttpServletRequest request, HttpServletResponse response, LocalDateTime requestedAt) {
        try {
            int statusCode = response.getStatus();
            boolean success = statusCode < 400;
            String errorType = (String) request.getAttribute("api.error.type");
            String errorMessage = (String) request.getAttribute("api.error.message");

            ApiLog apiLog = ApiLog.builder()
                    .requestedAt(requestedAt)
                    .success(success)
                    .statusCode(statusCode)
                    .errorType(errorType)
                    .errorMessage(errorMessage)
                    .userId(getCurrentUserId())
                    .ipAddress(getClientIp(request))
                    .method(request.getMethod())
                    .uri(request.getRequestURI())
                    .build();

            apiLogService.save(apiLog);
        } catch (Exception e) {
            log.error("API 로그 저장 중 오류 발생", e);
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return 0L;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        return 0L;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isBlankOrUnknown(ip)) ip = request.getHeader("Proxy-Client-IP");
        if (isBlankOrUnknown(ip)) ip = request.getHeader("WL-Proxy-Client-IP");
        if (isBlankOrUnknown(ip)) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    private boolean isBlankOrUnknown(String value) {
        return value == null || value.isBlank() || "unknown".equalsIgnoreCase(value);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/log")
                || uri.startsWith("/api/swagger-ui")
                || uri.startsWith("/api/v3/api-docs")
                || uri.startsWith("/api/api-docs")
                || uri.startsWith("/api/actuator");
    }
}
