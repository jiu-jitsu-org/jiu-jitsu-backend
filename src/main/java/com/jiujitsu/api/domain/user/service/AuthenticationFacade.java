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

    public Optional<User> getCurrentUserOptional() {
        // 현재 로그인 id
        Optional<Long> id = AuthenticationUtil.getCurrentUserId();
        return id.flatMap(userRepository::findById);
    }
}
