package com.jiujitsu.api.global.util;

import com.jiujitsu.api.global.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public class AuthenticationUtil {

    /**
     * SecurityContext에서 현재 인증된 사용자의 ID를 가져옵니다.
     * 
     * @return 현재 인증된 사용자의 ID
     * @throws RuntimeException 인증되지 않은 사용자인 경우
     */
    public static Optional<Long> getCurrentUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }


        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserPrincipal customUser) {
            return Optional.of(customUser.getUserId());
        }

        if (principal instanceof Long userId) {
            return Optional.of(userId);
        }

        return Optional.empty();
    }

    /**
     * 현재 사용자가 인증되어 있는지 확인합니다.
     * 
     * @return 인증 여부
     */
    public static boolean isAuthenticated() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    /**
     * 현재 인증된 사용자의 Authentication 객체를 가져옵니다.
     * 
     * @return Authentication 객체
     */
    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 현재 요청의 JWT 토큰을 가져옵니다.
     * 
     * @return JWT 토큰 (Bearer 접두사 제거된 상태), 토큰이 없으면 null
     */
    public static Optional<String> getCurrentToken() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return Optional.empty();
            }

            HttpServletRequest request = attributes.getRequest();
            String bearerToken = request.getHeader("Authorization");

            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                return Optional.of(bearerToken.substring(7));
            }

            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
