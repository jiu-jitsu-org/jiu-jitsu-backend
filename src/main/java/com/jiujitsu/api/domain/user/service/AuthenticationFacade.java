package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.util.AuthenticationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthenticationFacade {

    private final UserRepository userRepository;

    public void checkCurrentUser() {
        // 현재 로그인 id
        Long id = AuthenticationUtil.getCurrentUserId().orElseThrow(() -> new ErrorException(ErrorCode.LOGIN_NOT_ACCESS));
        // 유효한 유저인지 체크
        userRepository.findById(id).orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));
    }

    public User getCurrentUser() {
        // 현재 로그인 id
        Long id = AuthenticationUtil.getCurrentUserId().orElseThrow(() -> new ErrorException(ErrorCode.LOGIN_NOT_ACCESS));
        // 유효한 유저인지 체크
        return userRepository.findById(id).orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 현재 유저를 행 잠금(FOR UPDATE)으로 조회.
     * 같은 트랜잭션에서 getCurrentUser() 보다 먼저 호출해야 잠금 후의 최신 값을 읽는다 (영속성 컨텍스트 캐시).
     */
    public User getCurrentUserForUpdate() {
        Long id = AuthenticationUtil.getCurrentUserId().orElseThrow(() -> new ErrorException(ErrorCode.LOGIN_NOT_ACCESS));
        return userRepository.findByIdForUpdate(id).orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));
    }

    public Optional<User> getCurrentUserOptional() {
        // 현재 로그인 id
        Optional<Long> id = AuthenticationUtil.getCurrentUserId();
        return id.flatMap(userRepository::findById);
    }
}
