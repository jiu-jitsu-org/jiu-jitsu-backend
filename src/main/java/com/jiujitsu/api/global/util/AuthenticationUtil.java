package com.jiujitsu.api.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthenticationUtil {

    /**
     * SecurityContext에서 현재 인증된 사용자의 ID를 가져옵니다.
     * 
     * @return 현재 인증된 사용자의 ID
     * @throws RuntimeException 인증되지 않은 사용자인 경우
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증되지 않은 사용자입니다");
        }
        return (Long) authentication.getPrincipal();
    }

    /**
     * 현재 사용자가 인증되어 있는지 확인합니다.
     * 
     * @return 인증 여부
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * 현재 인증된 사용자의 Authentication 객체를 가져옵니다.
     * 
     * @return Authentication 객체
     */
    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
